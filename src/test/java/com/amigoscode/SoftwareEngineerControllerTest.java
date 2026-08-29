package com.amigoscode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
