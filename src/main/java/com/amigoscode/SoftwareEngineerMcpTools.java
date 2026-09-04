package com.amigoscode;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MCP adapter over {@link SoftwareEngineerService} — the same five operations the
 * REST controller exposes, reachable by an MCP client (Streamable-HTTP, {@code POST /mcp};
 * see {@code application.properties} and docs/ARCHITECTURE.md "The MCP server").
 *
 * <p>Both this and {@link SoftwareEngineerController} are thin adapters onto the one
 * {@code SoftwareEngineerService} bean, so persistence semantics (the tech-stack
 * {@code ArrayList} copy on update, the {@code existsById} guard on delete, the
 * null-id insert path) are identical on either entry point.
 *
 * <p><b>Validation.</b> Spring AI dispatches {@code @McpTool} methods reflectively —
 * there is no MVC argument resolver in that path, so {@code @Valid} / Bean Validation
 * on a parameter type does <em>not</em> fire. The {@code @Size} caps on
 * {@link CreateSoftwareEngineerRequest} / {@link UpdateSoftwareEngineerRequest} are
 * load-bearing (docs/ARCHITECTURE.md), so the write tools take flat parameters,
 * rebuild the request record, and run the {@link Validator} explicitly — the records
 * stay the single source of the constraints.
 *
 * <p><b>Errors.</b> Every failure mode is a {@link RuntimeException}: Spring AI turns
 * those into an error {@code CallToolResult} that the model sees and can react to. A
 * missing row reuses {@link SoftwareEngineerNotFoundException} (its
 * {@code @ResponseStatus} is inert outside MVC — harmless here); a bad id or a failed
 * constraint is an {@link IllegalArgumentException}, mirroring the REST layer's 400s.
 * Tool methods must not declare checked exceptions — those bypass the model-visible
 * error path. (Spring AI 2.0.1 renders the error text as {@code message + "\n" + cause.message};
 * with no distinct cause the line is simply repeated — cosmetic, the model still gets it.)
 */
@Component
public class SoftwareEngineerMcpTools {

    private final SoftwareEngineerService softwareEngineerService;
    private final Validator validator;

    public SoftwareEngineerMcpTools(SoftwareEngineerService softwareEngineerService, Validator validator) {
        this.softwareEngineerService = softwareEngineerService;
        this.validator = validator;
    }

    // annotations: read tools are read-only + non-destructive; create is neither idempotent nor
    // destructive; update replaces state (destructive) but repeats to the same result (idempotent);
    // delete is destructive and non-idempotent (a second call errors — the row is already gone).
    // openWorldHint=false everywhere — this is a bounded set of DB rows, not an open-ended system.

    @McpTool(name = "list-software-engineers",
            description = "List every software engineer, each with its id (UUID), name, and tech stack.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public List<SoftwareEngineer> listSoftwareEngineers() {
        return softwareEngineerService.getAllSoftwareEngineers();
    }

    @McpTool(name = "get-software-engineer",
            description = "Fetch one software engineer by id. The id is a UUID string as returned by "
                    + "list-software-engineers or create-software-engineer. Errors if no engineer has that id.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public SoftwareEngineer getSoftwareEngineer(
            @McpToolParam(description = "UUID of the engineer", required = true) String id) {
        UUID uuid = parseId(id);
        return softwareEngineerService.getSoftwareEngineerById(uuid)
                .orElseThrow(() -> new SoftwareEngineerNotFoundException(uuid));
    }

    @McpTool(name = "create-software-engineer",
            description = "Create a software engineer. 'name' is 1-255 characters; 'techStack' is a "
                    + "non-empty list of 1-50 technologies, each 1-255 characters. Returns the created "
                    + "engineer including its generated id.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    public SoftwareEngineer createSoftwareEngineer(
            @McpToolParam(description = "Full name, 1-255 characters", required = true) String name,
            @McpToolParam(description = "Technologies the engineer works with; 1-50 entries, each 1-255 characters",
                    required = true) List<String> techStack) {
        var request = new CreateSoftwareEngineerRequest(name, techStack);
        validate(request);
        return softwareEngineerService.insertSoftwareEngineer(request);
    }

    @McpTool(name = "update-software-engineer",
            description = "Replace a software engineer's name and tech stack (full replace, not a merge — "
                    + "both fields are required). The id is a UUID string; 'name' is 1-255 characters; "
                    + "'techStack' is a non-empty list of 1-50 technologies, each 1-255 characters. "
                    + "Errors if no engineer has that id. Returns the updated engineer.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = false))
    public SoftwareEngineer updateSoftwareEngineer(
            @McpToolParam(description = "UUID of the engineer to update", required = true) String id,
            @McpToolParam(description = "New full name, 1-255 characters", required = true) String name,
            @McpToolParam(description = "New tech stack; 1-50 entries, each 1-255 characters", required = true)
            List<String> techStack) {
        UUID uuid = parseId(id);
        var request = new UpdateSoftwareEngineerRequest(name, techStack);
        validate(request);
        return softwareEngineerService.updateSoftwareEngineerById(uuid, request)
                .orElseThrow(() -> new SoftwareEngineerNotFoundException(uuid));
    }

    @McpTool(name = "delete-software-engineer",
            description = "Delete a software engineer by id (a UUID string). Errors if no engineer has that id.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = false))
    public String deleteSoftwareEngineer(
            @McpToolParam(description = "UUID of the engineer to delete", required = true) String id) {
        UUID uuid = parseId(id);
        if (!softwareEngineerService.deleteSoftwareEngineerById(uuid)) {
            throw new SoftwareEngineerNotFoundException(uuid);
        }
        return "Deleted software engineer " + uuid;
    }

    /** Mirrors the REST layer's "non-UUID path segment -> 400": a bad id is a caller error, not a 404. */
    private UUID parseId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("id must be a UUID string, got: " + id);
        }
    }

    /** Runs Bean Validation that the reflective tool dispatch skips (see class Javadoc). */
    private void validate(Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Invalid request: " + detail);
        }
    }
}
