package com.amigoscode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
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
}
