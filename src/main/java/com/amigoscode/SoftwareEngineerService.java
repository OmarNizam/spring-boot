package com.amigoscode;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    // Empty when no row has that id; the controller turns that into a 404.
    // HTTP-status decisions stay in the controller, as on the create path.
    public Optional<SoftwareEngineer> getSoftwareEngineerById(UUID id) {
        return softwareEngineerRepository.findById(id);
    }

    public SoftwareEngineer insertSoftwareEngineer(CreateSoftwareEngineerRequest request) {
        // Build a fresh entity: id stays null, so save() inserts rather than merges.
        SoftwareEngineer engineer = new SoftwareEngineer(request.name(), request.techStack());
        return softwareEngineerRepository.save(engineer);
    }
}
