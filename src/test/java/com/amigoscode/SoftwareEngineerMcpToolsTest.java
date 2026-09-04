package com.amigoscode;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link SoftwareEngineerMcpTools}, the MCP adapter over
 * {@link SoftwareEngineerService}. Built with a real Bean Validation {@link Validator}
 * (the {@code @Size}/{@code @NotBlank}/{@code @NotEmpty} constraints on the request
 * records are load-bearing here — see the class Javadoc and docs/ARCHITECTURE.md) and a
 * mocked service, so every case asserts both the outcome and whether the service was
 * touched.
 */
@ExtendWith(MockitoExtension.class)
class SoftwareEngineerMcpToolsTest {

    private static final String LEN_256 = "a".repeat(256);

    @Mock
    private SoftwareEngineerService service;

    private SoftwareEngineerMcpTools tools;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        tools = new SoftwareEngineerMcpTools(service, validator);
    }

    // ---- listSoftwareEngineers ----

    @Test
    void listSoftwareEngineers_returnsServiceList() {
        List<SoftwareEngineer> engineers = List.of(
                new SoftwareEngineer(UUID.randomUUID(), "James", List.of("java")),
                new SoftwareEngineer(UUID.randomUUID(), "Jamila", List.of("python"))
        );
        given(service.getAllSoftwareEngineers()).willReturn(engineers);

        assertThat(tools.listSoftwareEngineers()).isEqualTo(engineers);
        then(service).should().getAllSoftwareEngineers();
    }

    // ---- getSoftwareEngineer ----

    @Test
    void getSoftwareEngineer_returnsEngineerAndDelegatesWithParsedUuid() {
        UUID id = UUID.randomUUID();
        SoftwareEngineer engineer = new SoftwareEngineer(id, "James", List.of("java"));
        given(service.getSoftwareEngineerById(id)).willReturn(Optional.of(engineer));

        assertThat(tools.getSoftwareEngineer(id.toString())).isSameAs(engineer);
        then(service).should().getSoftwareEngineerById(id);
    }

    @Test
    void getSoftwareEngineer_throwsNotFoundWhenServiceReturnsEmpty() {
        UUID id = UUID.randomUUID();
        given(service.getSoftwareEngineerById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tools.getSoftwareEngineer(id.toString()))
                .isInstanceOf(SoftwareEngineerNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void getSoftwareEngineer_throwsIllegalArgumentOnNonUuidAndNeverCallsService() {
        assertThatThrownBy(() -> tools.getSoftwareEngineer("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a UUID");

        then(service).shouldHaveNoInteractions();
    }

    // ---- createSoftwareEngineer ----

    @Test
    void createSoftwareEngineer_validRequestCapturesRequestAndReturnsSaved() {
        SoftwareEngineer saved = new SoftwareEngineer(UUID.randomUUID(), "Anne", List.of("java", "spring"));
        given(service.insertSoftwareEngineer(any(CreateSoftwareEngineerRequest.class))).willReturn(saved);

        SoftwareEngineer result = tools.createSoftwareEngineer("Anne", List.of("java", "spring"));

        assertThat(result).isSameAs(saved);
        ArgumentCaptor<CreateSoftwareEngineerRequest> captor =
                ArgumentCaptor.forClass(CreateSoftwareEngineerRequest.class);
        then(service).should().insertSoftwareEngineer(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Anne");
        assertThat(captor.getValue().techStack()).containsExactly("java", "spring");
    }

    @Test
    void createSoftwareEngineer_rejectsBlankNameAndNeverCallsService() {
        assertThatThrownBy(() -> tools.createSoftwareEngineer("  ", List.of("java")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid request");
        then(service).shouldHaveNoInteractions();
    }

    @Test
    void createSoftwareEngineer_rejectsOverLongNameAndNeverCallsService() {
        assertThatThrownBy(() -> tools.createSoftwareEngineer(LEN_256, List.of("java")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid request");
        then(service).shouldHaveNoInteractions();
    }

    @Test
    void createSoftwareEngineer_rejectsEmptyTechStackAndNeverCallsService() {
        assertThatThrownBy(() -> tools.createSoftwareEngineer("Anne", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid request");
        then(service).shouldHaveNoInteractions();
    }

    @Test
    void createSoftwareEngineer_rejectsOverSizedTechStackAndNeverCallsService() {
        List<String> tooMany = Collections.nCopies(51, "java");
        assertThatThrownBy(() -> tools.createSoftwareEngineer("Anne", tooMany))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid request");
        then(service).shouldHaveNoInteractions();
    }

    @Test
    void createSoftwareEngineer_rejectsBlankTechStackElementAndNeverCallsService() {
        assertThatThrownBy(() -> tools.createSoftwareEngineer("Anne", Arrays.asList("java", "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid request");
        then(service).shouldHaveNoInteractions();
    }

    @Test
    void createSoftwareEngineer_rejectsOverLongTechStackElementAndNeverCallsService() {
        assertThatThrownBy(() -> tools.createSoftwareEngineer("Anne", Arrays.asList("java", LEN_256)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid request");
        then(service).shouldHaveNoInteractions();
    }

    // ---- updateSoftwareEngineer ----

    @Test
    void updateSoftwareEngineer_validRequestParsesIdCapturesRequestAndReturnsUpdated() {
        UUID id = UUID.randomUUID();
        SoftwareEngineer updated = new SoftwareEngineer(id, "Anne Updated", List.of("java", "spring"));
        given(service.updateSoftwareEngineerById(eq(id), any(UpdateSoftwareEngineerRequest.class)))
                .willReturn(Optional.of(updated));

        SoftwareEngineer result =
                tools.updateSoftwareEngineer(id.toString(), "Anne Updated", List.of("java", "spring"));

        assertThat(result).isSameAs(updated);
        ArgumentCaptor<UpdateSoftwareEngineerRequest> captor =
                ArgumentCaptor.forClass(UpdateSoftwareEngineerRequest.class);
        then(service).should().updateSoftwareEngineerById(eq(id), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Anne Updated");
        assertThat(captor.getValue().techStack()).containsExactly("java", "spring");
    }

    @Test
    void updateSoftwareEngineer_throwsNotFoundWhenServiceReturnsEmpty() {
        UUID id = UUID.randomUUID();
        given(service.updateSoftwareEngineerById(eq(id), any(UpdateSoftwareEngineerRequest.class)))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> tools.updateSoftwareEngineer(id.toString(), "Anne", List.of("java")))
                .isInstanceOf(SoftwareEngineerNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateSoftwareEngineer_throwsIllegalArgumentOnBadIdAndNeverCallsService() {
        assertThatThrownBy(() -> tools.updateSoftwareEngineer("not-a-uuid", "Anne", List.of("java")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a UUID");
        then(service).shouldHaveNoInteractions();
    }

    @Test
    void updateSoftwareEngineer_rejectsBlankNameAndNeverCallsService() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> tools.updateSoftwareEngineer(id.toString(), "  ", List.of("java")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid request");
        then(service).should(never()).updateSoftwareEngineerById(any(), any());
    }

    // ---- deleteSoftwareEngineer ----

    @Test
    void deleteSoftwareEngineer_returnsConfirmationWhenServiceReturnsTrue() {
        UUID id = UUID.randomUUID();
        given(service.deleteSoftwareEngineerById(id)).willReturn(true);

        assertThat(tools.deleteSoftwareEngineer(id.toString()))
                .isEqualTo("Deleted software engineer " + id);
        then(service).should().deleteSoftwareEngineerById(id);
    }

    @Test
    void deleteSoftwareEngineer_throwsNotFoundWhenServiceReturnsFalse() {
        UUID id = UUID.randomUUID();
        given(service.deleteSoftwareEngineerById(id)).willReturn(false);

        assertThatThrownBy(() -> tools.deleteSoftwareEngineer(id.toString()))
                .isInstanceOf(SoftwareEngineerNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void deleteSoftwareEngineer_throwsIllegalArgumentOnBadIdAndNeverCallsService() {
        assertThatThrownBy(() -> tools.deleteSoftwareEngineer("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a UUID");
        then(service).shouldHaveNoInteractions();
    }
}
