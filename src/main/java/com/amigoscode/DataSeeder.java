package com.amigoscode;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final SoftwareEngineerRepository softwareEngineerRepository;

    public DataSeeder(SoftwareEngineerRepository softwareEngineerRepository) {
        this.softwareEngineerRepository = softwareEngineerRepository;
    }

    @Override
    public void run(String @NonNull ... args) {
        if (softwareEngineerRepository.count() > 0) {
            return;
        }

        List<SoftwareEngineer> softwareEngineers = List.of(
                new SoftwareEngineer(
                        "James",
                        Collections.singletonList("js, node, vue, java, spring boot")
                ),
                new SoftwareEngineer(
                        "Jamila",
                        Collections.singletonList("js, node, vue, python, FastApi")
                )
        );
        softwareEngineerRepository.saveAll(softwareEngineers);
    }
}
