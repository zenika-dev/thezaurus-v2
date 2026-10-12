package com.zenika.thezaurus.service;

import com.zenika.thezaurus.exception.ThezaurusException;
import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateException;
import jakarta.ws.rs.core.Response;
import java.util.Set;

public final class ReminderTemplateValidator {
    private ReminderTemplateValidator() {}

    public static void validateSyntax(
            Engine engine, String source, Set<String> allowedVariables, Set<String> allowedConditions) {
        int depth = 0;
        boolean[] hasElse = new boolean[20];
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '}') throw invalid("Accolade fermante Qute inattendue.");
            if (source.charAt(i) != '{') continue;
            int end = source.indexOf('}', i);
            if (end < 0) throw invalid("Balise Qute non fermée.");
            String token = source.substring(i + 1, end).trim();
            if (token.startsWith("#if ")) {
                if (!allowedConditions.contains(token.substring(4).trim()))
                    throw invalid("Condition Qute inconnue : " + token);
                if (++depth > 20) throw invalid("Trop de conditions imbriquées (maximum 20).");
                hasElse[depth - 1] = false;
            } else if (token.equals("/if")) {
                if (--depth < 0) throw invalid("Fermeture de condition Qute inattendue.");
            } else if (token.equals("#else")) {
                if (depth == 0 || hasElse[depth - 1]) throw invalid("Bloc else Qute inattendu ou multiple.");
                hasElse[depth - 1] = true;
            } else if (!allowedVariables.contains(token)) {
                throw invalid("Variable ou syntaxe Qute non autorisée : " + token);
            }
            i = end;
        }
        if (depth != 0) throw invalid("Condition Qute non fermée.");
        try {
            engine.parse(source);
        } catch (TemplateException exception) {
            throw invalid("Syntaxe Qute invalide : " + exception.getMessage());
        }
    }

    public static ThezaurusException invalid(String message) {
        return new ThezaurusException(message, Response.Status.BAD_REQUEST);
    }
}
