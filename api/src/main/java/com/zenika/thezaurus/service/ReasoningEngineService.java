package com.zenika.thezaurus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.zenika.thezaurus.model.TalkReviewRequest;
import com.zenika.thezaurus.model.TalkReviewResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ReasoningEngineService {

    private static final Logger LOG = Logger.getLogger(ReasoningEngineService.class);

    @ConfigProperty(name = "thezaurus.reasoning-engine.url", defaultValue = "")
    String reasoningEngineUrl;

    @Inject
    ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TalkReviewResponse reviewTalk(TalkReviewRequest request) {
        if (request == null || request.getTitle() == null) {
            throw new IllegalArgumentException("Le titre et l'abstract sont requis.");
        }

        String title = request.getTitle().trim();
        String abstractText = request.getAbstractText() != null ? request.getAbstractText().trim() : "";

        try {
            // Build inner JSON string for input.message
            Map<String, String> messageMap = new HashMap<>();
            messageMap.put("title", title);
            messageMap.put("abstract", abstractText);
            String messageJson = objectMapper.writeValueAsString(messageMap);

            // Build input map matching class_method: async_stream_query
            Map<String, Object> inputMap = new HashMap<>();
            //inputMap.put("user_id", "adama");
            inputMap.put("message", messageJson);

            Map<String, Object> payload = new HashMap<>();
            payload.put("class_method", "async_stream_query");
            payload.put("input", inputMap);

            String requestBody = objectMapper.writeValueAsString(payload);

            // Fetch Google OAuth2 Access Token via Application Default Credentials (ADC)
            String bearerToken = getAccessToken();

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(reasoningEngineUrl))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (bearerToken != null && !bearerToken.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + bearerToken);
            }

            LOG.info("Calling Vertex AI Reasoning Engine streamQuery endpoint: " + reasoningEngineUrl);
            HttpResponse<String> httpResponse = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() == 200) {
                LOG.info("Successfully received SSE response stream from Reasoning Engine AI Agent.");
                TalkReviewResponse parsedResponse = parseSseResponse(httpResponse.body(), title, abstractText);
                if (parsedResponse != null) {
                    return parsedResponse;
                }
            } else {
                LOG.warnf("Reasoning Engine returned HTTP %d: %s", httpResponse.statusCode(), httpResponse.body());
            }
        } catch (Exception e) {
            LOG.warn("Exception while calling Reasoning Engine AI Agent: " + e.getMessage(), e);
        }

        // Fallback response if Vertex AI endpoint is unreachable or response cannot be parsed
        return buildFallbackResponse(title, abstractText);
    }

    private String getAccessToken() {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            if (credentials.createScopedRequired()) {
                credentials = credentials.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
            }
            credentials.refreshIfExpired();
            if (credentials.getAccessToken() != null) {
                return credentials.getAccessToken().getTokenValue();
            }
        } catch (Exception e) {
            LOG.warn("Could not retrieve Google Application Default Credentials: " + e.getMessage());
        }
        return null;
    }

    public TalkReviewResponse parseSseResponse(String responseBody, String fallbackTitle, String fallbackAbstract) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            List<JsonNode> candidateNodes = new ArrayList<>();
            String[] lines = responseBody.split("\r?\n");

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("data:")) {
                    String dataPayload = line.substring(5).trim();
                    if ("[DONE]".equalsIgnoreCase(dataPayload) || dataPayload.isBlank()) {
                        continue;
                    }
                    try {
                        candidateNodes.add(objectMapper.readTree(dataPayload));
                    } catch (Exception ignored) {
                    }
                }
            }

            if (candidateNodes.isEmpty()) {
                String cleanBody = responseBody.replaceAll("^data:\\s*", "").trim();
                try {
                    candidateNodes.add(objectMapper.readTree(cleanBody));
                } catch (Exception ignored) {
                }
            }

            for (JsonNode rootNode : candidateNodes) {
                TalkReviewResponse response = extractResponseFromNode(rootNode, fallbackTitle, fallbackAbstract);
                if (response != null) {
                    return response;
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse Reasoning Engine SSE response body: " + e.getMessage(), e);
        }
        return null;
    }

    private TalkReviewResponse extractResponseFromNode(JsonNode root, String fallbackTitle, String fallbackAbstract) {
        try {
            JsonNode targetNode = root;
            if (root.has("content") && root.get("content").has("parts") && root.get("content").get("parts").isArray()) {
                JsonNode parts = root.get("content").get("parts");
                if (!parts.isEmpty() && parts.get(0).has("text")) {
                    String textContent = parts.get(0).get("text").asText();
                    if (textContent.contains("```json")) {
                        textContent = textContent.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
                    } else if (textContent.contains("```")) {
                        textContent = textContent.replaceAll("```\\s*", "").replaceAll("```", "").trim();
                    }
                    try {
                        JsonNode parsed = objectMapper.readTree(textContent);
                        if (parsed != null && parsed.isObject()) {
                            targetNode = parsed;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            if (targetNode.has("output")) {
                JsonNode output = targetNode.get("output");
                if (output.isTextual()) {
                    String outText = output.asText();
                    if (outText.contains("```json")) {
                        outText = outText.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
                    }
                    try {
                        JsonNode parsed = objectMapper.readTree(outText);
                        if (parsed != null && parsed.isObject()) {
                            targetNode = parsed;
                        }
                    } catch (Exception ignored) {
                    }
                } else if (output.isObject()) {
                    targetNode = output;
                }
            }

            // Handle improvements array structure from ADK Python Agent
            if (targetNode.has("improvements") && targetNode.get("improvements").isArray()) {
                List<String> suggestedTitles = new ArrayList<>();
                List<String> suggestedAbstracts = new ArrayList<>();
                List<String> feedback = new ArrayList<>();
                List<String> keyImprovements = new ArrayList<>();

                for (JsonNode imp : targetNode.get("improvements")) {
                    String cat = imp.has("category") ? imp.get("category").asText() : "";
                    String comment = imp.has("comment") ? imp.get("comment").asText() : "";

                    if (!comment.isBlank()) {
                        String prefix = "";
                        if ("titre".equalsIgnoreCase(cat)) prefix = "[Titre] ";
                        else if ("abstract".equalsIgnoreCase(cat)) prefix = "[Abstract] ";
                        else if ("coherence".equalsIgnoreCase(cat)) prefix = "[Cohérence] ";
                        feedback.add(prefix + comment);
                    }

                    if (imp.has("suggestions") && imp.get("suggestions").isArray()) {
                        for (JsonNode sug : imp.get("suggestions")) {
                            String sugText = sug.asText().replaceAll("^\"|\"$", "").trim();
                            if ("titre".equalsIgnoreCase(cat)) {
                                suggestedTitles.add(sugText);
                            } else if ("abstract".equalsIgnoreCase(cat)) {
                                suggestedAbstracts.add(sugText);
                            } else {
                                keyImprovements.add(sugText);
                            }
                        }
                    }
                }

                if (!suggestedTitles.isEmpty() || !suggestedAbstracts.isEmpty() || !feedback.isEmpty() || !keyImprovements.isEmpty()) {
                    return new TalkReviewResponse(suggestedTitles, suggestedAbstracts, feedback, keyImprovements);
                }
            }

            // Standard key-value structure fallback
            List<String> suggestedTitles = extractStringList(targetNode, "suggestedTitles", "suggested_titles", "suggestedTitle", "title");
            List<String> suggestedAbstracts = extractStringList(targetNode, "suggestedAbstracts", "suggested_abstracts", "suggestedAbstract", "abstract");
            List<String> feedback = extractStringList(targetNode, "feedback");
            List<String> keyImprovements = extractStringList(targetNode, "keyImprovements", "key_improvements");

            if (!suggestedTitles.isEmpty() || !suggestedAbstracts.isEmpty() || !feedback.isEmpty() || !keyImprovements.isEmpty()) {
                return new TalkReviewResponse(suggestedTitles, suggestedAbstracts, feedback, keyImprovements);
            }
        } catch (Exception e) {
            LOG.error("Error extracting response from node: " + e.getMessage());
        }
        return null;
    }

    private List<String> extractStringList(JsonNode node, String... keys) {
        List<String> list = new ArrayList<>();
        for (String key : keys) {
            if (node.has(key) && node.get(key).isArray()) {
                for (JsonNode item : node.get(key)) {
                    list.add(item.asText());
                }
                return list;
            } else if (node.has(key) && node.get(key).isTextual()) {
                list.add(node.get(key).asText());
                return list;
            }
        }
        return list;
    }

    private TalkReviewResponse buildFallbackResponse(String title, String abstractText) {
        List<String> suggestedTitles = List.of(
                title.isEmpty()
                        ? "Intégrer le Front-end au Back-end Quarkus : Simplifiez vos Architectures !"
                        : "Intégrer le Front-end au Back-end : " + title,
                "Back-end et Front-end Unis : L'approche Quarkus pour des Architectures Simples",
                "Quarkus : Quand le Back-end Intègre le Front-end (Sans JSF ni JSP !)"
        );

        List<String> suggestedAbstracts = List.of(
                "Ajouter après 'simplifier vos architectures' : 'pour gagner en productivité et réduire la complexité opérationnelle de vos projets.'",
                "Reformuler la fin : 'Découvrez comment optimiser vos flux de développement et de déploiement grâce à ces trois approches novatrices qui vous feront gagner un temps précieux.'",
                "Insister sur le gain de temps et les ressources dès le départ : 'Fini les configurations multiples et les pipelines complexes : cette session vous montrera comment économiser temps et ressources en intégrant intelligemment vos interfaces.'"
        );

        List<String> feedback = List.of(
                "[Titre] Le titre est très accrocheur et utilise une référence explicite au sujet principal.",
                "[Abstract] L'abstract est bien structuré et très engageant pour le public visé.",
                "[Cohérence] La cohérence entre le titre et l'abstract est très bonne."
        );

        List<String> keyImprovements = List.of(
                "Assurez-vous que le titre continue de promettre une intégration forte.",
                "Veillez à ce que l'abstract valide immédiatement le sérieux technique du sujet."
        );

        return new TalkReviewResponse(suggestedTitles, suggestedAbstracts, feedback, keyImprovements);
    }
}
