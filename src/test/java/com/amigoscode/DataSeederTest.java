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
import static org.assertj.core.api.Assertions.tuple;
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

    /**
     * Pins the current seed shape: one comma-joined string per engineer, even though
     * {@code techStack} is an {@code @ElementCollection} that could hold each technology
     * as its own element. Documents what is there today — update this when the seed data
     * is normalised.
     */
    @Test
    void seedsTwoEngineersWhenTableIsEmpty() {
        given(softwareEngineerRepository.count()).willReturn(0L);

        dataSeeder.run();

        verify(softwareEngineerRepository).saveAll(engineersCaptor.capture());
        assertThat(engineersCaptor.getValue())
                .extracting(SoftwareEngineer::getName, SoftwareEngineer::getTechStack)
                .containsExactly(
                        tuple("James", List.of("js, node, vue, java, spring boot")),
                        tuple("Jamila", List.of("js, node, vue, python, FastApi"))
                );
    }

    @Test
    void doesNothingWhenTableAlreadyHasRows() {
        given(softwareEngineerRepository.count()).willReturn(2L);

        dataSeeder.run();

        verify(softwareEngineerRepository, never()).saveAll(anyList());
    }
}
