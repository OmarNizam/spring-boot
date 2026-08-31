package com.amigoscode;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PostMapping
    public ResponseEntity<SoftwareEngineer> createSoftwareEngineer(
            @Valid @RequestBody CreateSoftwareEngineerRequest request) {
        SoftwareEngineer created = softwareEngineerService.insertSoftwareEngineer(request);
        // 201 with the persisted entity so the caller learns the generated id.
        // No Location header: there is no GET-by-id endpoint to point it at yet.
        //
        // Taint analysis flags `created` as request-derived data reaching a response
        // sink. Not a real XSS vector here: @RestController serialises via Jackson as
        // application/json, which browsers never render as markup, and the request
        // fields are length-bounded by CreateSoftwareEngineerRequest's @Size.
        //noinspection JvmTaintAnalysis
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
