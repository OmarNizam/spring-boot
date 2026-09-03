package com.amigoscode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for updating a {@link SoftwareEngineer} by id.
 *
 * <p>Same shape as {@link CreateSoftwareEngineerRequest} and deliberately not the JPA
 * entity, for the same reason: it carries no {@code id}, so a client cannot use the body
 * to redirect the update onto a different row. The id comes only from the path variable;
 * the service loads that row and overwrites its {@code name}/{@code techStack}, so this
 * is a full replace (PUT semantics) rather than a partial merge — a field omitted from
 * the request clears the corresponding column rather than leaving it untouched.
 *
 * <p>The {@code @Size} bounds mirror the create path for the same reason: the list cap
 * bounds how many rows land in {@code software_engineer_tech_stack}, and 255 matches
 * Hibernate's default {@code varchar} length so an over-long value is a clean 400 rather
 * than a database constraint violation.
 */
public record UpdateSoftwareEngineerRequest(
        @NotBlank @Size(max = 255) String name,
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 255) String> techStack
) {
}
