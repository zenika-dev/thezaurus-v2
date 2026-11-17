package com.zenika.thezaurus.visuals.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@RegisterForReflection
public record BlogVisualRequest(
        @NotBlank(message = "Le titre du blog post est requis")
        String title,

        @NotEmpty(message = "Au moins un auteur (writer) est requis") @JsonAlias("writer")
        List<PersonInput> writers) {}
