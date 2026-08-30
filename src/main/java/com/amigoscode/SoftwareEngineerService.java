package com.amigoscode;

import org.springframework.stereotype.Service;

import java.util.List;

//This class will handle everything related to business logic
@Service
public class SoftwareEngineerService {

    private final SoftwareEngineerRepository softwareEngineerRepository;

    public SoftwareEngineerService(SoftwareEngineerRepository softwareEngineerRepository) {
        this.softwareEngineerRepository = softwareEngineerRepository;
    }

    public List<SoftwareEngineer> getAllSoftwareEngineers() {
       return  softwareEngineerRepository.findAll();
    }

    public SoftwareEngineer insertSoftwareEngineer(CreateSoftwareEngineerRequest request) {
        // Build a fresh entity: id stays null, so save() inserts rather than merges.
        SoftwareEngineer engineer = new SoftwareEngineer(request.name(), request.techStack());
        return softwareEngineerRepository.save(engineer);
    }
}
