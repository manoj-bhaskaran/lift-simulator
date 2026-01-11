package com.liftsimulator.admin.controller;

import com.liftsimulator.admin.dto.CreateLiftSystemRequest;
import com.liftsimulator.admin.dto.LiftSystemResponse;
import com.liftsimulator.admin.dto.UpdateLiftSystemRequest;
import com.liftsimulator.admin.service.LiftSystemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing lift systems.
 */
@RestController
@RequestMapping("/api/lift-systems")
public class LiftSystemController {

    private final LiftSystemService liftSystemService;

    public LiftSystemController(LiftSystemService liftSystemService) {
        this.liftSystemService = liftSystemService;
    }

    /**
     * Creates a new lift system.
     *
     * @param request the creation request
     * @return the created lift system with 201 status
     */
    @PostMapping
    public ResponseEntity<LiftSystemResponse> createLiftSystem(
        @Valid @RequestBody CreateLiftSystemRequest request
    ) {
        LiftSystemResponse response = liftSystemService.createLiftSystem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all lift systems.
     *
     * @return list of all lift systems
     */
    @GetMapping
    public ResponseEntity<List<LiftSystemResponse>> getAllLiftSystems() {
        List<LiftSystemResponse> systems = liftSystemService.getAllLiftSystems();
        return ResponseEntity.ok(systems);
    }

    /**
     * Retrieves a lift system by ID.
     *
     * @param id the system ID
     * @return the lift system details
     */
    @GetMapping("/{id}")
    public ResponseEntity<LiftSystemResponse> getLiftSystemById(@PathVariable Long id) {
        LiftSystemResponse response = liftSystemService.getLiftSystemById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates lift system metadata.
     *
     * @param id the system ID
     * @param request the update request
     * @return the updated lift system
     */
    @PutMapping("/{id}")
    public ResponseEntity<LiftSystemResponse> updateLiftSystem(
        @PathVariable Long id,
        @Valid @RequestBody UpdateLiftSystemRequest request
    ) {
        LiftSystemResponse response = liftSystemService.updateLiftSystem(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a lift system and all its versions.
     *
     * @param id the system ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLiftSystem(@PathVariable Long id) {
        liftSystemService.deleteLiftSystem(id);
        return ResponseEntity.noContent().build();
    }
}
