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
    public ResponseEntity<SoftwareEngineer> getEngineerById(@PathVariable UUID id) {
        // 404 on a well-formed but unknown id; a non-UUID path segment never gets
        // here — Spring's type conversion fails it as a framework-default 400.
        return softwareEngineerService.getSoftwareEngineerById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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
        // Taint analysis flags `created` as request-derived data reaching a response
        // sink. Not a real XSS vector here: @RestController serialises via Jackson as
        // application/json, which browsers never render as markup.
        //noinspection JvmTaintAnalysis
        return ResponseEntity.created(location).body(created);
    }
}
