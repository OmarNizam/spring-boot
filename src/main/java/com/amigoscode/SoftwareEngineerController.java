package com.amigoscode;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/software-engineers")
public class SoftwareEngineerController {

    private final SoftwareEngineerService softwareEngineerService;

    public SoftwareEngineerController(SoftwareEngineerService softwareEngineerService) {
        this.softwareEngineerService =  softwareEngineerService;
    }

    @GetMapping
    public List<SoftwareEngineer> getEngineers() {
        return softwareEngineerService.getAllSoftwareEngineers();
    }

    @GetMapping("{id}")
    public SoftwareEngineer getEngineerById(@PathVariable UUID id) {
        // Unknown id -> SoftwareEngineerNotFoundException, which carries
        // @ResponseStatus(NOT_FOUND) so Spring renders a 404. A non-UUID path
        // segment never gets here — type conversion fails it as a default 400.
        return softwareEngineerService.getSoftwareEngineerById(id)
                .orElseThrow(() -> new SoftwareEngineerNotFoundException(id));
    }

    @PostMapping
    public ResponseEntity<SoftwareEngineer> createSoftwareEngineer(
            @Valid @RequestBody CreateSoftwareEngineerRequest request) {
        SoftwareEngineer created = softwareEngineerService.insertSoftwareEngineer(request);
        // 201 with the persisted entity so the caller learns the generated id, plus a
        // Location header pointing at the new GET-by-id resource.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        // Taint analysis flags `created` (and `location`, via fromCurrentRequest())
        // as request-derived data reaching a response sink. Not a real XSS vector
        // here: @RestController serialises the body via Jackson as application/json,
        // and Location is a header, not an HTML sink — browsers never render either
        // as markup.
        //noinspection JvmTaintAnalysis
        return ResponseEntity.created(location).body(created);
    }
}
