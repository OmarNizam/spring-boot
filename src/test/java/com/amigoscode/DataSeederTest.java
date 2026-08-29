package com.amigoscode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private SoftwareEngineerRepository softwareEngineerRepository;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Captor
    private ArgumentCaptor<List<SoftwareEngineer>> engineersCaptor;

    @Test
    void seedsTwoEngineersWhenTableIsEmpty() {
        given(softwareEngineerRepository.count()).willReturn(0L);

        dataSeeder.run();

        verify(softwareEngineerRepository).saveAll(engineersCaptor.capture());
        assertThat(engineersCaptor.getValue())
                .extracting(SoftwareEngineer::getName)
                .containsExactly("James", "Jamila");
    }

    @Test
    void doesNothingWhenTableAlreadyHasRows() {
        given(softwareEngineerRepository.count()).willReturn(2L);

        dataSeeder.run();

        verify(softwareEngineerRepository, never()).saveAll(anyList());
    }
}
