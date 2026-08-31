package com.amigoscode;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when a lookup by id finds no row. {@code @ResponseStatus} lets Spring's
 * {@code ResponseStatusExceptionResolver} turn it into a {@code 404} on its own, so
 * this works before a {@code @RestControllerAdvice} exists (docs/ARCHITECTURE.md
 * "Still open"). Being a distinct type — rather than a bare
 * {@code ResponseStatusException} — it stays catchable by name once one is added.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class SoftwareEngineerNotFoundException extends RuntimeException {

    public SoftwareEngineerNotFoundException(UUID id) {
        super("No software engineer with id " + id);
    }
}
