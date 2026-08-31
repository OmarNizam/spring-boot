package com.amigoscode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for creating a {@link SoftwareEngineer}.
 *
 * <p>Deliberately <em>not</em> the JPA entity: it carries no {@code id}, so a
 * client cannot supply one and turn a create into an overwrite/merge of an
 * existing row (see docs/ARCHITECTURE.md "Known rough edges"). The service maps
 * this to a fresh entity with a {@code null} id, which is what forces
 * {@code JpaRepository.save} down the insert path.
 *
 * <p>The {@code @Size} bounds cap what an unauthenticated caller can persist in a
 * single request; 255 matches Hibernate's default {@code varchar} length for the
 * mapped columns. They also give static taint analysis a sanitising constraint on
 * the way in.
 */
public record CreateSoftwareEngineerRequest(
        @NotBlank @Size(max = 255) String name,
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 255) String> techStack
) {
}
