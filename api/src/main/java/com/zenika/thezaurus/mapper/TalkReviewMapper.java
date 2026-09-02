package com.zenika.thezaurus.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenika.thezaurus.model.AgentResponse;
import com.zenika.thezaurus.model.ImprovementItem;
import com.zenika.thezaurus.model.TalkReviewResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TalkReviewMapper {

    private static final Logger LOG = Logger.getLogger(TalkReviewMapper.class);
    private static final Pattern MARKDOWN_JSON_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");

    @Inject
    ObjectMapper objectMapper;

    public Optional<TalkReviewResponse> toDomain(String sseResponseBody) {
        if (sseResponseBody == null || sseResponseBody.isBlank()) {
            return Optional.empty();
        }

        try {
            String jsonPayload = extractJsonPayloadFromSse(sseResponseBody);
            if (jsonPayload.isBlank()) {
                return Optional.empty();
            }

            AgentResponse agentResponse = objectMapper.readValue(jsonPayload, AgentResponse.class);

            return Optional.ofNullable(mapToTalkReviewResponse(agentResponse));

        } catch (Exception e) {
            LOG.warnf("Erreur de mapping du JSON de l'Agent : %s", e.getMessage());
            return Optional.empty();
        }
    }

    private String extractJsonPayloadFromSse(String sseBody) {
        for (String line : sseBody.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("data:")) {
                String data = trimmed.substring(5).trim();
                if (!"[DONE]".equalsIgnoreCase(data) && !data.isBlank()) {
                    return unwrapContentIfNeeded(data);
                }
            }
        }
        return unwrapContentIfNeeded(sseBody.replaceAll("^data:\\s*", "").trim());
    }

    private String unwrapContentIfNeeded(String rawJson) {
        var matcher = MARKDOWN_JSON_PATTERN.matcher(rawJson);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (root.has("content")
                    && root.get("content").has("parts")
                    && root.get("content").get("parts").isArray()) {
                JsonNode parts = root.get("content").get("parts");
                if (!parts.isEmpty() && parts.get(0).has("text")) {
                    return unwrapContentIfNeeded(parts.get(0).get("text").asText());
                }
            }
            if (root.has("output") && root.get("output").isTextual()) {
                return unwrapContentIfNeeded(root.get("output").asText());
            }
        } catch (Exception ignored) {
        }

        return rawJson;
    }

    private TalkReviewResponse mapToTalkReviewResponse(AgentResponse response) {
        if (response == null
                || response.improvements() == null
                || response.improvements().isEmpty()) {
            return null;
        }

        List<String> suggestedTitles = new ArrayList<>();
        List<String> suggestedAbstracts = new ArrayList<>();
        List<String> feedback = new ArrayList<>();
        List<String> keyImprovements = new ArrayList<>();

        for (ImprovementItem item : response.improvements()) {
            String category = item.category() != null ? item.category().toLowerCase() : "";
            String comment = item.comment() != null ? item.comment().trim() : "";

            if (!comment.isBlank()) {
                String prefix =
                        switch (category) {
                            case "titre" -> "[Titre] ";
                            case "abstract" -> "[Abstract] ";
                            case "coherence" -> "[Cohérence] ";
                            default -> "";
                        };
                feedback.add(prefix + comment);
            }

            if (item.suggestions() != null) {
                for (String suggestion : item.suggestions()) {
                    String cleanSuggestion =
                            suggestion.replaceAll("^\"|\"$", "").trim();
                    switch (category) {
                        case "titre" -> suggestedTitles.add(cleanSuggestion);
                        case "abstract" -> suggestedAbstracts.add(cleanSuggestion);
                        default -> keyImprovements.add(cleanSuggestion);
                    }
                }
            }
        }

        return new TalkReviewResponse(suggestedTitles, suggestedAbstracts, feedback, keyImprovements);
    }
}
