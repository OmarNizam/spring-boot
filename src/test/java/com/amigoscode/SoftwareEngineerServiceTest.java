package com.amigoscode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SoftwareEngineerServiceTest {

    @Mock
    private SoftwareEngineerRepository softwareEngineerRepository;

    @InjectMocks
    private SoftwareEngineerService softwareEngineerService;

    @Test
    void getAllSoftwareEngineers_returnsRepositoryFindAllResult() {
        List<SoftwareEngineer> engineers = List.of(
                new SoftwareEngineer(UUID.randomUUID(), "James", List.of("java")),
                new SoftwareEngineer(UUID.randomUUID(), "Jamila", List.of("python"))
        );
        given(softwareEngineerRepository.findAll()).willReturn(engineers);

        List<SoftwareEngineer> result = softwareEngineerService.getAllSoftwareEngineers();

        assertThat(result).isEqualTo(engineers);
        verify(softwareEngineerRepository).findAll();
    }

    @Test
    void getAllSoftwareEngineers_returnsEmptyListWhenRepositoryIsEmpty() {
        given(softwareEngineerRepository.findAll()).willReturn(List.of());

        assertThat(softwareEngineerService.getAllSoftwareEngineers()).isEmpty();
        verify(softwareEngineerRepository).findAll();
    }

    @Test
    void getSoftwareEngineerById_returnsRepositoryFindByIdResult() {
        UUID id = UUID.randomUUID();
        SoftwareEngineer engineer = new SoftwareEngineer(id, "James", List.of("java"));
        given(softwareEngineerRepository.findById(id)).willReturn(Optional.of(engineer));

        assertThat(softwareEngineerService.getSoftwareEngineerById(id)).contains(engineer);
        verify(softwareEngineerRepository).findById(id);
    }

    @Test
    void getSoftwareEngineerById_returnsEmptyWhenRepositoryHasNoSuchRow() {
        UUID id = UUID.randomUUID();
        given(softwareEngineerRepository.findById(id)).willReturn(Optional.empty());

        assertThat(softwareEngineerService.getSoftwareEngineerById(id)).isEmpty();
        verify(softwareEngineerRepository).findById(id);
    }

    @Test
    void insertSoftwareEngineer_savesNewEntityWithNullIdAndReturnsSaved() {
        CreateSoftwareEngineerRequest request =
                new CreateSoftwareEngineerRequest("Anne", List.of("java", "spring"));
        SoftwareEngineer persisted =
                new SoftwareEngineer(UUID.randomUUID(), "Anne", List.of("java", "spring"));
        given(softwareEngineerRepository.save(any(SoftwareEngineer.class))).willReturn(persisted);

        SoftwareEngineer result = softwareEngineerService.insertSoftwareEngineer(request);

        assertThat(result).isSameAs(persisted);
        ArgumentCaptor<SoftwareEngineer> captor = ArgumentCaptor.forClass(SoftwareEngineer.class);
        verify(softwareEngineerRepository).save(captor.capture());
        // id must be null so JpaRepository.save() inserts instead of merging an existing row.
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getName()).isEqualTo("Anne");
        assertThat(captor.getValue().getTechStack()).containsExactly("java", "spring");
    }

    @Test
    void deleteSoftwareEngineerById_deletesAndReturnsTrueWhenRowExists() {
        UUID id = UUID.randomUUID();
        given(softwareEngineerRepository.existsById(id)).willReturn(true);

        assertThat(softwareEngineerService.deleteSoftwareEngineerById(id)).isTrue();
        verify(softwareEngineerRepository).deleteById(id);
    }

    @Test
    void deleteSoftwareEngineerById_returnsFalseAndDoesNotDeleteWhenNoSuchRow() {
        UUID id = UUID.randomUUID();
        given(softwareEngineerRepository.existsById(id)).willReturn(false);

        assertThat(softwareEngineerService.deleteSoftwareEngineerById(id)).isFalse();
        verify(softwareEngineerRepository, never()).deleteById(any());
    }
}
