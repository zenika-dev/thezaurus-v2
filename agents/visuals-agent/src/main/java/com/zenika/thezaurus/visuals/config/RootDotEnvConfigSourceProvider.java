package com.zenika.thezaurus.visuals.config;

import io.smallrye.config.DotEnvConfigSourceProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

/**
 * Charge automatiquement le fichier {@code .env} situé dans les répertoires parents
 * (notamment à la racine du dépôt {@code ../../.env} lorsque l'agent est lancé depuis {@code agents/visuals-agent}).
 */
public class RootDotEnvConfigSourceProvider implements ConfigSourceProvider {

    private static final List<Path> PARENT_DOTENV_CANDIDATES = List.of(Path.of("../.env"), Path.of("../../.env"));

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        List<ConfigSource> sources = new ArrayList<>();
        for (Path candidate : PARENT_DOTENV_CANDIDATES) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized)) {
                sources.addAll(DotEnvConfigSourceProvider.dotEnvSources(
                        normalized.toUri().toString(), forClassLoader));
            }
        }
        return sources;
    }
}
