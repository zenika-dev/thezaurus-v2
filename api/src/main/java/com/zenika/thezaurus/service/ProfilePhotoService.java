package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProfilePhotoService {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> FORMATS = Set.of("jpeg", "png", "webp");
    private static final Logger LOG = Logger.getLogger(ProfilePhotoService.class);
    HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Inject
    UserRepository users;

    @Inject
    ProfilePhotoStorage storage;

    public record Photo(byte[] bytes, String contentType) {}

    public User requireUser(String email) throws ExecutionException, InterruptedException {
        User user = users.findByEmail(email.toLowerCase(Locale.ROOT));
        if (user == null) throw new NotFoundException();
        return user;
    }

    /** Point d’entrée des futurs agents, volontairement sans endpoint réseau pour l’instant. */
    public Photo getForAgent(String email) throws ExecutionException, InterruptedException {
        User user = requireUser(email);
        if (!user.allowsAiPhoto()) throw new ForbiddenException("Utilisation de la photo par l’IA refusée.");
        Photo photo = read(user);
        // Relecture après le stockage distant : un retrait pendant le téléchargement s’applique aussi.
        if (!requireUser(email).allowsAiPhoto()) throw new ForbiddenException();
        return prepareForAgent(photo);
    }

    /** Bound the copy sent to agents; a normalized PNG can be larger than the original upload. */
    static Photo prepareForAgent(Photo photo) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(photo.bytes()));
            if (image == null) throw new BadRequestException("Photo invalide.");
            int longest = Math.max(image.getWidth(), image.getHeight());
            if (longest <= 1536) return photo;
            double scale = 1536.0 / longest;
            BufferedImage resized = new BufferedImage(
                    Math.max(1, (int) Math.round(image.getWidth() * scale)),
                    Math.max(1, (int) Math.round(image.getHeight() * scale)),
                    "image/jpeg".equals(photo.contentType())
                            ? BufferedImage.TYPE_INT_RGB
                            : BufferedImage.TYPE_INT_ARGB);
            var graphics = resized.createGraphics();
            try {
                graphics.setRenderingHint(
                        java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.drawImage(image, 0, 0, resized.getWidth(), resized.getHeight(), null);
            } finally {
                graphics.dispose();
            }
            return encode(resized, "image/jpeg".equals(photo.contentType()));
        } catch (IOException e) {
            throw new BadRequestException("Photo invalide.", e);
        }
    }

    public Photo read(User user) {
        if (user.hasCustomPhoto()) {
            byte[] bytes = storage.read(user.customPhotoObject());
            // Detect the stored format, including historical PNGs without an extension.
            try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw new BadRequestException("Photo invalide.");
                var reader = readers.next();
                try {
                    return new Photo(
                            bytes, "JPEG".equalsIgnoreCase(reader.getFormatName()) ? "image/jpeg" : "image/png");
                } finally {
                    reader.dispose();
                }
            } catch (IOException e) {
                throw new BadRequestException("Photo invalide.", e);
            }
        }
        if (user.googlePhotoUrl() == null || user.googlePhotoUrl().isBlank()) throw new NotFoundException();
        URI uri = trustedGooglePhoto(user.googlePhotoUrl());
        try {
            for (int redirects = 0; redirects <= 3; redirects++) {
                HttpResponse<InputStream> response = http.send(
                        HttpRequest.newBuilder(uri)
                                .timeout(Duration.ofSeconds(10))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream input = response.body()) {
                    int status = response.statusCode();
                    if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                        if (redirects == 3)
                            throw new WebApplicationException("Trop de redirections de la photo Google.", 502);
                        String location = response.headers()
                                .firstValue("Location")
                                .orElseThrow(() -> new WebApplicationException("Redirection Google invalide.", 502));
                        // La validation s’applique à chaque saut, avant toute nouvelle connexion.
                        uri = trustedGooglePhoto(uri.resolve(location).toString());
                        continue;
                    }
                    if (status == 404) throw new NotFoundException();
                    if (status != 200) throw new WebApplicationException("Photo Google indisponible.", 502);
                    return normalize(input.readNBytes(MAX_BYTES + 1));
                }
            }
            throw new WebApplicationException("Photo Google indisponible.", 502);
        } catch (IOException e) {
            throw new WebApplicationException("Photo Google indisponible.", e, 502);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WebApplicationException("Photo Google indisponible.", e, 502);
        }
    }

    public void upload(User user, InputStream input) throws IOException, ExecutionException, InterruptedException {
        Photo photo = normalize(input.readNBytes(MAX_BYTES + 1));
        String object = storage.save(photo.bytes(), photo.contentType());
        String previous;
        try {
            previous = users.updateCustomPhoto(user.email(), object);
        } catch (ExecutionException | InterruptedException | RuntimeException e) {
            deleteQuietly(object);
            throw e;
        }
        deleteQuietly(previous);
    }

    public void reset(User user) throws ExecutionException, InterruptedException {
        String previous = users.updateCustomPhoto(user.email(), "");
        deleteQuietly(previous);
    }

    public void syncGoogle(User user, String url) throws ExecutionException, InterruptedException {
        if (!user.hasCustomPhoto()) {
            if (url != null && !url.isBlank()) trustedGooglePhoto(url);
            users.syncGooglePhoto(user.email(), url == null ? "" : url);
        }
    }

    private void deleteQuietly(String object) {
        try {
            storage.delete(object);
        } catch (RuntimeException e) {
            LOG.warn("Ancienne photo à nettoyer dans le stockage privé", e);
        }
    }

    static URI trustedGooglePhoto(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme())
                    || host == null
                    || !(host.equals("googleusercontent.com") || host.endsWith(".googleusercontent.com"))
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Photo Google invalide.");
        }
    }

    /** Réencode les pixels seuls : JPEG pour les JPEG, PNG pour préserver la transparence des autres formats. */
    static Photo normalize(byte[] bytes) {
        if (bytes.length > MAX_BYTES) throw new WebApplicationException("La photo dépasse 5 Mo.", 413);
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new BadRequestException("Photo JPEG, PNG ou WebP attendue.");
            var reader = readers.next();
            try {
                if (!FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT)))
                    throw new BadRequestException("Photo JPEG, PNG ou WebP attendue.");
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 20_000_000)
                    throw new BadRequestException("Photo trop grande : maximum 20 mégapixels.");
                BufferedImage image = reader.read(0);
                return encode(image, "JPEG".equalsIgnoreCase(reader.getFormatName()));
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new BadRequestException("Photo invalide.", e);
        }
    }

    private static Photo encode(BufferedImage image, boolean jpeg) throws IOException {
        var output = new ByteArrayOutputStream();
        if (!jpeg) {
            ImageIO.write(image, "png", output);
            return new Photo(output.toByteArray(), "image/png");
        }
        var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (var stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            var parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(0.9f);
            // No source metadata is passed to the writer (EXIF, GPS, comments, etc.).
            writer.write(null, new javax.imageio.IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
        return new Photo(output.toByteArray(), "image/jpeg");
    }
}
