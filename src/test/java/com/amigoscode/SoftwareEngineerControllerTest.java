package com.amigoscode;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SoftwareEngineerController.class)
class SoftwareEngineerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SoftwareEngineerService softwareEngineerService;

    @Test
    void getEngineers_returnsServiceResultAsJsonArray() throws Exception {
        UUID id = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        given(softwareEngineerService.getAllSoftwareEngineers()).willReturn(
                List.of(new SoftwareEngineer(id, "James", List.of("java", "spring boot")))
        );

        mockMvc.perform(get("/api/v1/software-engineers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("James"))
                .andExpect(jsonPath("$[0].techStack", contains("java", "spring boot")));
    }

    @Test
    void getEngineers_returnsEmptyArrayWhenServiceHasNone() throws Exception {
        given(softwareEngineerService.getAllSoftwareEngineers()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/software-engineers"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getEngineerById_returnsEngineerAsJsonWhenFound() throws Exception {
        UUID id = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        given(softwareEngineerService.getSoftwareEngineerById(id)).willReturn(
                Optional.of(new SoftwareEngineer(id, "James", List.of("java", "spring boot")))
        );

        mockMvc.perform(get("/api/v1/software-engineers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("James"))
                .andExpect(jsonPath("$.techStack", contains("java", "spring boot")));
    }

    @Test
    void getEngineerById_returns404WhenServiceHasNoSuchEngineer() throws Exception {
        UUID id = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        given(softwareEngineerService.getSoftwareEngineerById(id)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/software-engineers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(SoftwareEngineerNotFoundException.class));
    }

    /**
     * A non-UUID path segment fails Spring's {@code String}→{@code UUID} conversion as
     * {@code MethodArgumentTypeMismatchException} before the handler runs — a different path
     * from the 404 above. Pins the framework-default 400; there is no
     * {@code @RestControllerAdvice} shaping it yet (docs/ARCHITECTURE.md "Still open").
     * Rewrite to assert the chosen body once one exists.
     */
    @Test
    void getEngineerById_rejectsNonUuidIdWith400() throws Exception {
        mockMvc.perform(get("/api/v1/software-engineers/not-a-uuid"))
                .andExpect(status().isBadRequest());

        verify(softwareEngineerService, never()).getSoftwareEngineerById(any());
    }

    /**
     * Pins that a service failure is <em>not</em> translated: there is no
     * {@code @RestControllerAdvice} (docs/ARCHITECTURE.md lists one as future work), so the
     * exception propagates straight out of the dispatcher. This test documents the gap and
     * will fail loudly — by design — the moment error handling is added, at which point it
     * should be rewritten to assert the chosen HTTP status.
     */
    @Test
    void getEngineers_propagatesServiceExceptionUnhandled() {
        given(softwareEngineerService.getAllSoftwareEngineers())
                .willThrow(new RuntimeException("repository is down"));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/software-engineers")))
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("repository is down");
    }

    @Test
    void createSoftwareEngineer_persistsRequestAndReturns201WithGeneratedId() throws Exception {
        UUID generated = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        given(softwareEngineerService.insertSoftwareEngineer(any()))
                .willReturn(new SoftwareEngineer(generated, "Anne", List.of("java", "spring")));

        mockMvc.perform(post("/api/v1/software-engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Anne", "techStack": ["java", "spring"]}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/software-engineers/" + generated)))
                .andExpect(jsonPath("$.id").value(generated.toString()))
                .andExpect(jsonPath("$.name").value("Anne"))
                .andExpect(jsonPath("$.techStack", contains("java", "spring")));

        ArgumentCaptor<CreateSoftwareEngineerRequest> captor =
                ArgumentCaptor.forClass(CreateSoftwareEngineerRequest.class);
        verify(softwareEngineerService).insertSoftwareEngineer(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Anne");
        assertThat(captor.getValue().techStack()).containsExactly("java", "spring");
    }

    /**
     * The create body binds to {@link CreateSoftwareEngineerRequest}, which has no {@code id}
     * field, so a client-supplied {@code id} cannot reach the persistence layer and turn a
     * create into an overwrite of an existing row. Jackson drops the unknown property.
     */
    @Test
    void createSoftwareEngineer_ignoresClientSuppliedId() throws Exception {
        given(softwareEngineerService.insertSoftwareEngineer(any()))
                .willReturn(new SoftwareEngineer(UUID.randomUUID(), "Anne", List.of("java")));

        mockMvc.perform(post("/api/v1/software-engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": "11111111-1111-1111-1111-111111111111", "name": "Anne", "techStack": ["java"]}"""))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateSoftwareEngineerRequest> captor =
                ArgumentCaptor.forClass(CreateSoftwareEngineerRequest.class);
        verify(softwareEngineerService).insertSoftwareEngineer(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Anne");
    }

    @Test
    void createSoftwareEngineer_rejectsBlankNameWith400() throws Exception {
        mockMvc.perform(post("/api/v1/software-engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  ", "techStack": ["java"]}"""))
                .andExpect(status().isBadRequest());

        verify(softwareEngineerService, never()).insertSoftwareEngineer(any());
    }

    @Test
    void createSoftwareEngineer_rejectsEmptyTechStackWith400() throws Exception {
        mockMvc.perform(post("/api/v1/software-engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Anne", "techStack": []}"""))
                .andExpect(status().isBadRequest());

        verify(softwareEngineerService, never()).insertSoftwareEngineer(any());
    }

    /**
     * {@code @NotEmpty} on the list only guards its size; {@code List<@NotBlank String>} is a
     * separate container-element constraint. Pins that a whitespace-only entry is rejected
     * before the service runs, so a blank tech string never reaches the side table.
     */
    @Test
    void createSoftwareEngineer_rejectsBlankTechStackElementWith400() throws Exception {
        mockMvc.perform(post("/api/v1/software-engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Anne", "techStack": ["java", "  "]}"""))
                .andExpect(status().isBadRequest());

        verify(softwareEngineerService, never()).insertSoftwareEngineer(any());
    }

    /**
     * {@code @Size(max = 255)} on {@code name} bounds what a single request can persist.
     * Pins that an over-long name is rejected before the service runs.
     */
    @Test
    void createSoftwareEngineer_rejectsOversizeNameWith400() throws Exception {
        String tooLong = "a".repeat(256);

        mockMvc.perform(post("/api/v1/software-engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "techStack": ["java"]}""".formatted(tooLong)))
                .andExpect(status().isBadRequest());

        verify(softwareEngineerService, never()).insertSoftwareEngineer(any());
    }

    /**
     * {@code @Size(max = 50)} on the list itself bounds how many rows a single request can
     * insert into {@code software_engineer_tech_stack}. Pins that an over-long list is
     * rejected before the service runs.
     */
    @Test
    void createSoftwareEngineer_rejectsOversizeTechStackWith400() throws Exception {
        String tooMany = ("\"t\",".repeat(51)).replaceAll(",$", "");

        mockMvc.perform(post("/api/v1/software-engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Anne", "techStack": [%s]}""".formatted(tooMany)))
                .andExpect(status().isBadRequest());

        verify(softwareEngineerService, never()).insertSoftwareEngineer(any());
    }

    /**
     * {@code List<@Size(max = 255) String>} is a container-element constraint, separate from
     * the list-size cap above. Pins that a single over-long tech string is rejected before
     * the service runs, so nothing wider than the {@code tech} column reaches the side table.
     */
    @Test
    void createSoftwareEngineer_rejectsOversizeTechStackElementWith400() throws Exception {
        String tooLong = "a".repeat(256);

        mockMvc.perform(post("/api/v1/software-engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Anne", "techStack": ["java", "%s"]}""".formatted(tooLong)))
                .andExpect(status().isBadRequest());

        verify(softwareEngineerService, never()).insertSoftwareEngineer(any());
    }

    @Test
    void deleteEngineerById_returns204WhenServiceRemovedARow() throws Exception {
        UUID id = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        given(softwareEngineerService.deleteSoftwareEngineerById(id)).willReturn(true);

        mockMvc.perform(delete("/api/v1/software-engineers/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void deleteEngineerById_returns404WhenServiceRemovedNothing() throws Exception {
        UUID id = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        given(softwareEngineerService.deleteSoftwareEngineerById(id)).willReturn(false);

        mockMvc.perform(delete("/api/v1/software-engineers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(SoftwareEngineerNotFoundException.class));
    }

    /**
     * A non-UUID path segment fails Spring's {@code String}→{@code UUID} conversion as a
     * framework-default 400 before the handler runs — same path as the GET-by-id case.
     * Pins that the service is never consulted for a malformed id.
     */
    @Test
    void deleteEngineerById_rejectsNonUuidIdWith400() throws Exception {
        mockMvc.perform(delete("/api/v1/software-engineers/not-a-uuid"))
                .andExpect(status().isBadRequest());

        verify(softwareEngineerService, never()).deleteSoftwareEngineerById(any());
    }

    /**
     * A body Jackson cannot parse fails as {@code HttpMessageNotReadableException} — a
     * different path from the bean-validation 400s above. Pins the framework-default 400;
     * there is no {@code @RestControllerAdvice} shaping it yet (docs/ARCHITECTURE.md
     * "The write path" / "Still open"). Rewrite to assert the chosen body once one exists.
     */
    @Test
    void createSoftwareEngineer_rejectsMalformedJsonWith400() throws Exception {
        mockMvc.perform(post("/api/v1/software-engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Anne\","))
                .andExpect(status().isBadRequest());

        verify(softwareEngineerService, never()).insertSoftwareEngineer(any());
    }
}
