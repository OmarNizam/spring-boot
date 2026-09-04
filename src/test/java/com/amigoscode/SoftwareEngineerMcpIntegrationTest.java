package com.amigoscode;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test of the MCP server layer ({@link SoftwareEngineerMcpTools} exposed by
 * Spring AI over Streamable-HTTP at {@code POST /mcp}; see {@code application.properties}
 * and docs/ARCHITECTURE.md "The MCP server"). It drives the server with the real MCP Java
 * SDK client, so it exercises the whole path a model-side client would: protocol
 * handshake, tool discovery, and tool invocation over HTTP.
 *
 * <p>Unlike {@link SoftwareEngineerMcpToolsTest} (which unit-tests the component with a
 * mocked service) this runs against real Postgres on host port 5332 — same precondition
 * as {@link SoftwareEngineerRepositoryTest}; {@code spring.docker.compose.skip.in-tests}
 * is true so the suite will not start it. Because the client talks to the server across
 * HTTP on a real servlet container, {@code @Transactional} would not roll anything back,
 * so the CRUD test creates and then deletes its own row.
 *
 * <p>This is the first test in the suite to boot a real servlet container
 * ({@link ApplicationTests} uses the default MOCK web environment), so a context-startup
 * failure involving the web tier or the MCP auto-configuration surfaces here first.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SoftwareEngineerMcpIntegrationTest {

    private static final List<String> EXPECTED_TOOL_NAMES = List.of(
            "list-software-engineers", "get-software-engineer", "create-software-engineer",
            "update-software-engineer", "delete-software-engineer");

    /** Pulls the generated {@code id} out of a returned engineer JSON payload. */
    private static final Pattern ID_IN_JSON =
            Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    @LocalServerPort
    private int port;

    /**
     * Streamable-HTTP sessions carry server-side state, so each test gets its own freshly
     * initialized client; the caller closes it.
     */
    private McpSyncClient newClient() {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint("/mcp")
                .build();
        McpSyncClient client = McpClient.sync(transport).build();
        client.initialize();
        return client;
    }

    private static boolean isError(CallToolResult result) {
        return Boolean.TRUE.equals(result.isError());
    }

    private static String textOf(CallToolResult result) {
        return ((TextContent) result.content().get(0)).text();
    }

    @Test
    void listTools_exposesTheFiveToolsWithSchemas() {
        try (McpSyncClient client = newClient()) {
            List<Tool> tools = client.listTools().tools();

            assertThat(tools).extracting(Tool::name)
                    .containsExactlyInAnyOrderElementsOf(EXPECTED_TOOL_NAMES);
            assertThat(tools).allSatisfy(tool ->
                    assertThat(tool.description()).isNotBlank());

            Tool create = tools.stream()
                    .filter(t -> t.name().equals("create-software-engineer")).findFirst().orElseThrow();
            assertThat(propertyNames(create)).contains("name", "techStack");
            assertThat(requiredNames(create)).contains("name", "techStack");

            Tool get = tools.stream()
                    .filter(t -> t.name().equals("get-software-engineer")).findFirst().orElseThrow();
            assertThat(requiredNames(get)).contains("id");
        }
    }

    @Test
    void listSoftwareEngineers_returnsSeededEngineers() {
        try (McpSyncClient client = newClient()) {
            CallToolResult result = client.callTool(new CallToolRequest("list-software-engineers", Map.of()));

            assertThat(isError(result)).isFalse();
            assertThat(textOf(result)).contains("James").contains("Jamila");
        }
    }

    /**
     * Full lifecycle over MCP: create returns a generated id, get/update/get reflect the
     * writes, delete confirms by id, and a get after delete is a model-visible error result.
     * A random name suffix keeps reruns from colliding on nothing in particular, and the
     * final delete is the test's own cleanup (no rollback across HTTP).
     */
    @Test
    void crudRoundTrip_createGetUpdateDelete() {
        String name = "Integration MCP " + UUID.randomUUID().toString().substring(0, 8);
        try (McpSyncClient client = newClient()) {
            CallToolResult created = client.callTool(new CallToolRequest("create-software-engineer",
                    Map.of("name", name, "techStack", List.of("kotlin"))));
            assertThat(isError(created)).isFalse();
            String createdText = textOf(created);
            assertThat(createdText).contains(name).contains("kotlin");
            String id = extractId(createdText);

            CallToolResult fetched = client.callTool(
                    new CallToolRequest("get-software-engineer", Map.of("id", id)));
            assertThat(isError(fetched)).isFalse();
            assertThat(textOf(fetched)).contains(name);

            String newName = name + " (updated)";
            CallToolResult updated = client.callTool(new CallToolRequest("update-software-engineer",
                    Map.of("id", id, "name", newName, "techStack", List.of("scala"))));
            assertThat(isError(updated)).isFalse();

            CallToolResult refetched = client.callTool(
                    new CallToolRequest("get-software-engineer", Map.of("id", id)));
            assertThat(textOf(refetched)).contains(newName).contains("scala");

            CallToolResult deleted = client.callTool(
                    new CallToolRequest("delete-software-engineer", Map.of("id", id)));
            assertThat(isError(deleted)).isFalse();
            assertThat(textOf(deleted)).contains("Deleted software engineer " + id);

            CallToolResult afterDelete = client.callTool(
                    new CallToolRequest("get-software-engineer", Map.of("id", id)));
            assertThat(isError(afterDelete)).isTrue();
        }
    }

    @Test
    void getSoftwareEngineer_unknownId_returnsErrorResult() {
        try (McpSyncClient client = newClient()) {
            CallToolResult result = client.callTool(new CallToolRequest(
                    "get-software-engineer", Map.of("id", UUID.randomUUID().toString())));

            assertThat(isError(result)).isTrue();
            assertThat(textOf(result)).contains("No software engineer with id");
        }
    }

    @Test
    void createSoftwareEngineer_blankName_returnsErrorResult() {
        try (McpSyncClient client = newClient()) {
            CallToolResult result = client.callTool(new CallToolRequest("create-software-engineer",
                    Map.of("name", "  ", "techStack", List.of("x"))));

            assertThat(isError(result)).isTrue();
            assertThat(textOf(result)).contains("Invalid request");
        }
    }

    @Test
    void getSoftwareEngineer_malformedId_returnsErrorResult() {
        try (McpSyncClient client = newClient()) {
            CallToolResult result = client.callTool(new CallToolRequest(
                    "get-software-engineer", Map.of("id", "not-a-uuid")));

            assertThat(isError(result)).isTrue();
            assertThat(textOf(result)).contains("must be a UUID");
        }
    }

    private static String extractId(String json) {
        Matcher m = ID_IN_JSON.matcher(json);
        assertThat(m.find()).as("id in payload: %s", json).isTrue();
        return m.group(1);
    }

    @SuppressWarnings("unchecked")
    private static List<String> propertyNames(Tool tool) {
        Object properties = tool.inputSchema().get("properties");
        assertThat(properties).as("inputSchema.properties of %s", tool.name()).isInstanceOf(Map.class);
        return List.copyOf(((Map<String, Object>) properties).keySet());
    }

    @SuppressWarnings("unchecked")
    private static List<String> requiredNames(Tool tool) {
        Object required = tool.inputSchema().get("required");
        assertThat(required).as("inputSchema.required of %s", tool.name()).isInstanceOf(List.class);
        return (List<String>) required;
    }
}
