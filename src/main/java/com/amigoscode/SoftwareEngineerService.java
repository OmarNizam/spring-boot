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

    // Empty when no row has that id; the controller turns that into a 404, same seam as
    // the read-by-id and delete paths. Loads the existing row first rather than building
    // a new entity with the path id set on it: the loaded entity's id is guaranteed to
    // match a real row, so save() below merges onto it rather than risking an insert of
    // an entity carrying a caller-supplied id that JpaRepository.save can't tell from "new".
    public Optional<SoftwareEngineer> updateSoftwareEngineerById(UUID id, UpdateSoftwareEngineerRequest request) {
        return softwareEngineerRepository.findById(id)
                .map(engineer -> {
                    engineer.setName(request.name());
                    engineer.setTechStack(request.techStack());
                    return softwareEngineerRepository.save(engineer);
                });
    }

    // true when a row was removed, false when no row had that id; the controller
    // turns false into a 404. HTTP-status decisions stay in the controller, as on
    // the read-by-id and create paths.
    //
    // existsById + deleteById is two statements with a TOCTOU gap: a concurrent
    // delete of the same id could make deleteById a no-op after existsById saw the
    // row. Harmless here (the outcome — row gone, caller told so — is the same) and
    // left un-guarded rather than wrapped in @Transactional, an idiom nothing else
    // in this app uses. deleteById itself is a silent no-op on an unknown id in
    // Spring Data JPA, so the guard is what produces the 404, not a caught exception.
    public boolean deleteSoftwareEngineerById(UUID id) {
        if (!softwareEngineerRepository.existsById(id)) {
            return false;
        }
        softwareEngineerRepository.deleteById(id);
        return true;
    }
}
