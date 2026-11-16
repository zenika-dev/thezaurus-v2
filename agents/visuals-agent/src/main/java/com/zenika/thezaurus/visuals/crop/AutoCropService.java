package com.zenika.thezaurus.visuals.crop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenika.thezaurus.visuals.crop.CropGeometry.Analysis;
import com.zenika.thezaurus.visuals.crop.CropGeometry.Box;
import com.zenika.thezaurus.visuals.crop.CropGeometry.Crop;
import com.zenika.thezaurus.visuals.crop.CropGeometry.Plan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** One image understanding call, deterministic framing, no image generation. */
@ApplicationScoped
public class AutoCropService {
    private static final int CACHE_LIMIT = 64;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private final Settings settings;
    private final ObjectMapper mapper;
    private final DetectionClient client;
    private final LinkedHashMap<String, Analysis> cache = new LinkedHashMap<>(64, .75f, true);
    private final ConcurrentHashMap<String, CompletableFuture<Detection>> pending = new ConcurrentHashMap<>();
    private final Semaphore slots = new Semaphore(4);

    public record Settings(
            boolean enabled,
            String model,
            String apiKey,
            String project,
            String location,
            String credentialFile,
            int maxAttempts,
            int timeoutMs) {
        public Settings {
            model = model == null || model.isBlank() ? "gemini-3.1-flash-lite" : model.trim();
            apiKey = apiKey == null ? "" : apiKey.trim();
            project = project == null ? "" : project.trim();
            location = location == null || location.isBlank() ? "global" : location.trim();
            credentialFile = credentialFile == null ? "" : credentialFile.trim();
            maxAttempts = Math.max(1, Math.min(3, maxAttempts));
            timeoutMs = Math.max(1000, Math.min(120000, timeoutMs));
            if (!model.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,100}")
                    || (!project.isEmpty() && !project.matches("[a-z0-9][a-z0-9-]{0,62}"))
                    || !location.matches("global|[a-z]+-[a-z]+[0-9]+")) {
                throw new IllegalArgumentException("Configuration Gemini invalide.");
            }
        }
    }

    public record Usage(long inputTokens, long outputTokens, long totalTokens) {
        public static Usage zero() {
            return new Usage(0, 0, 0);
        }

        public Usage plus(Usage other) {
            return new Usage(
                    inputTokens + other.inputTokens,
                    outputTokens + other.outputTokens,
                    totalTokens + other.totalTokens);
        }
    }

    public record CropResult(
            Crop crop,
            String status,
            Analysis analysis,
            List<String> issues,
            Usage usage,
            int calls,
            boolean cacheHit) {
        public CropResult {
            issues = List.copyOf(issues);
        }
    }

    public record DetectionResponse(String json, Usage usage) {
        public DetectionResponse {
            if (usage == null) usage = Usage.zero();
        }
    }

    @FunctionalInterface
    public interface DetectionClient {
        DetectionResponse detect(byte[] analysisJpeg, String correction);
    }

    private record Detection(Analysis analysis, List<String> issues, Usage usage, int calls) {}

    @Inject
    public AutoCropService(
            ObjectMapper mapper,
            @ConfigProperty(name = "visuals.crop.enabled", defaultValue = "true") boolean enabled,
            @ConfigProperty(name = "visuals.crop.model", defaultValue = "gemini-3.1-flash-lite") String model,
            @ConfigProperty(name = "visuals.crop.api-key") Optional<String> apiKey,
            @ConfigProperty(name = "visuals.crop.project") Optional<String> project,
            @ConfigProperty(name = "visuals.crop.location", defaultValue = "global") String location,
            @ConfigProperty(name = "visuals.crop.credentials-file") Optional<String> credentialFile,
            @ConfigProperty(name = "visuals.crop.max-attempts", defaultValue = "3") int maxAttempts,
            @ConfigProperty(name = "visuals.crop.timeout-ms", defaultValue = "20000") int timeoutMs) {
        this(
                new Settings(
                        enabled,
                        model,
                        apiKey.orElse(""),
                        project.orElse(""),
                        location,
                        credentialFile.orElse(""),
                        maxAttempts,
                        timeoutMs),
                mapper,
                null);
    }

    /** Explicit transport seam for tests: no credentials or network needed. */
    public AutoCropService(Settings settings, ObjectMapper mapper, DetectionClient client) {
        this.settings = settings;
        this.mapper = mapper;
        this.client = client == null ? new GeminiCropClient(settings, mapper) : client;
    }

    public boolean enabled() {
        return settings.enabled;
    }

    public boolean available() {
        return settings.enabled && (!settings.apiKey.isBlank() || !settings.project.isBlank());
    }

    public String model() {
        return settings.model;
    }

    public String provider() {
        return settings.apiKey.isBlank() ? "vertex" : "developer";
    }

    public CropResult analyze(
            byte[] photo, int imageWidth, int imageHeight, int frameWidth, int frameHeight, int radius) {
        if (!settings.enabled)
            return new CropResult(Crop.centered(), "disabled", null, List.of(), Usage.zero(), 0, false);
        if (!available())
            throw new CropException(
                    503, "NOT_CONFIGURED", "Configurer Gemini ou le projet Vertex AI pour le cadrage automatique.");
        validateImage(photo, imageWidth, imageHeight);
        if (frameWidth <= 0 || frameHeight <= 0 || radius < 0 || radius > Math.min(frameWidth, frameHeight) / 2) {
            throw new CropException(400, "INVALID_FRAME", "Dimensions du cadre invalides.");
        }
        String key = digest(photo);
        Analysis previous;
        synchronized (cache) {
            previous = cache.get(key);
        }
        if (previous != null)
            return result(previous, Usage.zero(), 0, true, imageWidth, imageHeight, frameWidth, frameHeight, radius);

        CompletableFuture<Detection> mine = new CompletableFuture<>();
        CompletableFuture<Detection> existing = pending.putIfAbsent(key, mine);
        if (existing != null) {
            Detection detection;
            try {
                detection = existing.join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof CropException safe) throw safe;
                throw new CropException(502, "PROVIDER_UNAVAILABLE", "Analyse Gemini indisponible.");
            }
            if (detection.analysis == null)
                return new CropResult(Crop.centered(), "fallback", null, detection.issues, Usage.zero(), 0, true);
            return result(
                    detection.analysis,
                    Usage.zero(),
                    0,
                    true,
                    imageWidth,
                    imageHeight,
                    frameWidth,
                    frameHeight,
                    radius);
        }
        try {
            // Another owner may have completed between our cache read and ownership.
            synchronized (cache) {
                previous = cache.get(key);
            }
            if (previous != null) {
                mine.complete(new Detection(previous, List.of(), Usage.zero(), 0));
                return result(
                        previous, Usage.zero(), 0, true, imageWidth, imageHeight, frameWidth, frameHeight, radius);
            }
            if (!slots.tryAcquire())
                throw new CropException(503, "BUSY", "Des analyses sont déjà en cours. Réessayer plus tard.");
            Detection detection;
            try {
                detection = detect(normalizedJpeg(photo));
            } finally {
                slots.release();
            }
            if (detection.analysis != null) {
                synchronized (cache) {
                    cache.put(key, detection.analysis);
                    while (cache.size() > CACHE_LIMIT)
                        cache.remove(cache.keySet().iterator().next());
                }
            }
            mine.complete(detection);
            if (detection.analysis == null)
                return new CropResult(
                        Crop.centered(), "fallback", null, detection.issues, detection.usage, detection.calls, false);
            return result(
                    detection.analysis,
                    detection.usage,
                    detection.calls,
                    false,
                    imageWidth,
                    imageHeight,
                    frameWidth,
                    frameHeight,
                    radius);
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            pending.remove(key, mine);
        }
    }

    private CropResult result(
            Analysis analysis, Usage usage, int calls, boolean cacheHit, int iw, int ih, int fw, int fh, int radius) {
        Plan plan = CropGeometry.plan(analysis, iw, ih, fw, fh, radius);
        // Valid detection + impossible geometry is final, never another paid call.
        return new CropResult(plan.crop(), plan.status(), analysis, plan.issues(), usage, calls, cacheHit);
    }

    private Detection detect(byte[] jpeg) {
        Usage usage = Usage.zero();
        String issue = "Aucune détection valide reçue de Gemini.";
        for (int call = 1; call <= settings.maxAttempts; call++) {
            if (Thread.currentThread().isInterrupted())
                throw new CropException(503, "CANCELLED", "Analyse Gemini interrompue.");
            DetectionResponse response = client.detect(jpeg, call == 1 ? "" : issue);
            usage = usage.plus(response.usage);
            try {
                Analysis analysis = parseNativeAnalysis(response.json);
                return new Detection(analysis, List.of(), usage, call);
            } catch (IllegalArgumentException e) {
                issue =
                        "Coordonnées invalides : utiliser des rectangles 0..1000 non vides dans l'image, avec le visage inclus dans le sujet.";
            }
        }
        return new Detection(null, List.of(issue), usage, settings.maxAttempts);
    }

    Analysis parseNativeAnalysis(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            if (root == null || !root.isObject() || !root.has("subject") || !root.has("face"))
                throw new IllegalArgumentException();
            return new Analysis(
                    nativeBox(root.get("subject")), root.get("face").isNull() ? null : nativeBox(root.get("face")));
        } catch (Exception e) {
            throw new IllegalArgumentException("Réponse de détection invalide.");
        }
    }

    private Box nativeBox(JsonNode node) {
        if (node == null || !node.isObject()) throw new IllegalArgumentException();
        double[] values = new double[4];
        String[] keys = {"x", "y", "width", "height"};
        for (int i = 0; i < keys.length; i++) {
            JsonNode value = node.get(keys[i]);
            if (value == null
                    || !value.isNumber()
                    || !Double.isFinite(value.asDouble())
                    || value.asDouble() < 0
                    || value.asDouble() > 1000) {
                throw new IllegalArgumentException();
            }
            values[i] = value.asDouble() / 1000;
        }
        return new Box(values[0], values[1], values[2], values[3]);
    }

    private static String digest(byte[] photo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(photo));
        } catch (Exception e) {
            throw new IllegalStateException("Empreinte d'image indisponible.");
        }
    }

    private static void validateImage(byte[] photo, int width, int height) {
        if (photo == null
                || photo.length == 0
                || photo.length > MAX_IMAGE_BYTES
                || width <= 0
                || height <= 0
                || (long) width * height > 40_000_000)
            throw new CropException(400, "INVALID_IMAGE", "Photo ou dimensions invalides.");
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(photo))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException();
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                if (reader.getWidth(0) != width || reader.getHeight(0) != height) throw new IllegalArgumentException();
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            throw new CropException(400, "INVALID_IMAGE", "La photo et ses dimensions ne correspondent pas.");
        }
    }

    private static byte[] normalizedJpeg(byte[] photo) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(photo));
            double ratio = Math.min(1, 1024d / Math.max(source.getWidth(), source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * ratio));
            int height = Math.max(1, (int) Math.round(source.getHeight() * ratio));
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(source, 0, 0, width, height, null);
            } finally {
                g.dispose();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(scaled, "jpeg", output)) throw new IllegalArgumentException();
            return output.toByteArray();
        } catch (Exception e) {
            throw new CropException(400, "INVALID_IMAGE", "Impossible de préparer la photo pour l'analyse.");
        }
    }
}
