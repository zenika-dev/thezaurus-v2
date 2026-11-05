package com.zenika.thezaurus.visuals.client;

import java.util.List;

/**
 * Port d'abstraction vers le SDK Google GenAI ({@code com.google.genai}) afin de permettre
 * le remplacement ou le mock complet dans les tests automatisés sans consommer de crédits IA.
 */
public interface GenAiVisualClient {

    record InlineMediaPart(byte[] data, String mimeType) {}

    record GeneratedImage(byte[] data, String mimeType) {}

    GeneratedImage generateImage(String model, String prompt, List<InlineMediaPart> mediaParts);
}
