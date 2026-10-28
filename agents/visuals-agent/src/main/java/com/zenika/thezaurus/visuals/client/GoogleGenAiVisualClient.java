package com.zenika.thezaurus.visuals.client;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import com.zenika.thezaurus.visuals.exception.VisualGenerationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Implémentation reposant sur la librairie officielle Google GenAI ({@code com.google.genai}).
 */
@ApplicationScoped
public class GoogleGenAiVisualClient implements GenAiVisualClient {

    private static final Logger LOG = Logger.getLogger(GoogleGenAiVisualClient.class);

    private final Optional<String> apiKey;
    private final boolean useVertexAi;
    private final Optional<String> projectId;
    private final String location;
    private final int timeoutMs;

    @Inject
    public GoogleGenAiVisualClient(
            @ConfigProperty(name = "visuals.gemini.api-key") Optional<String> apiKey,
            @ConfigProperty(name = "visuals.gemini.vertex-ai", defaultValue = "false") boolean useVertexAi,
            @ConfigProperty(name = "visuals.gemini.project") Optional<String> projectId,
            @ConfigProperty(name = "visuals.gemini.location", defaultValue = "europe-west1") String location,
            @ConfigProperty(name = "visuals.gemini.timeout-ms", defaultValue = "120000") int timeoutMs) {
        this.apiKey = apiKey;
        this.useVertexAi = useVertexAi;
        this.projectId = projectId;
        this.location = location;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public GeneratedImage generateImage(String model, String prompt, List<InlineMediaPart> mediaParts) {
        Content content = buildContent(prompt, mediaParts);
        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseModalities("TEXT", "IMAGE")
                .build();

        LOG.infof("Appel à Google GenAI avec le modèle '%s' (%d média(s) en entrée)", model, mediaParts.size());
        try (Client client = createClient()) {
            GenerateContentResponse response = client.models.generateContent(model, content, config);
            return extractImage(response);
        } catch (VisualGenerationException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "Erreur lors de la génération d'image avec Google GenAI (modèle=%s)", model);
            throw new VisualGenerationException(
                    "Échec de la génération du visuel auprès de Google GenAI : " + e.getMessage(), e);
        }
    }

    Content buildContent(String prompt, List<InlineMediaPart> mediaParts) {
        List<Part> parts = new ArrayList<>();
        parts.add(Part.fromText(prompt));
        if (mediaParts != null) {
            for (InlineMediaPart media : mediaParts) {
                parts.add(Part.fromBytes(media.data(), media.mimeType()));
            }
        }
        return Content.fromParts(parts.toArray(new Part[0]));
    }

    GeneratedImage extractImage(GenerateContentResponse response) {
        if (response == null) {
            throw new VisualGenerationException("Réponse vide reçue depuis Google GenAI.");
        }

        ImmutableList<Part> parts = response.parts();
        if (parts != null) {
            for (Part part : parts) {
                if (part.inlineData().isPresent()) {
                    Blob blob = part.inlineData().get();
                    if (blob.data().isPresent() && blob.data().get().length > 0) {
                        String mimeType = blob.mimeType().orElse("image/png");
                        return new GeneratedImage(blob.data().get(), mimeType);
                    }
                }
            }
        }

        throw new VisualGenerationException("Aucune image n'a été retournée dans la réponse du modèle Gemini.");
    }

    protected Client createClient() {
        Client.Builder builder = Client.builder()
                .httpOptions(HttpOptions.builder().timeout(timeoutMs).build());

        if (useVertexAi) {
            builder.vertexAI(true);
            projectId.filter(p -> !p.isBlank()).ifPresent(builder::project);
            if (location != null && !location.isBlank()) {
                builder.location(location);
            }
        } else if (apiKey.isPresent() && !apiKey.get().isBlank()) {
            builder.apiKey(apiKey.get().trim());
        }

        return builder.build();
    }
}
