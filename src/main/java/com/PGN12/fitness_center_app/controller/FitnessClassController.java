package com.PGN12.fitness_center_app.controller;

import com.PGN12.fitness_center_app.model.FitnessClass;
import com.PGN12.fitness_center_app.service.FitnessClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for managing Fitness Classes.
 * Endpoints for admin to manage classes and for users to view schedule.
 */
@RestController
@RequestMapping("/api/classes")
public class FitnessClassController {

    private final FitnessClassService fitnessClassService;

    @Autowired
    public FitnessClassController(FitnessClassService fitnessClassService) {
        this.fitnessClassService = fitnessClassService;
    }

    /**
     * Creates a new fitness class (Admin operation).
     * Corresponds to "Create: Add new classes ... to classes.txt"
     * Endpoint: POST /api/classes
     * @param fitnessClass The class data from request body.
     * @return ResponseEntity with created class or error.
     */
    @PostMapping
    public ResponseEntity<?> createClass(@RequestBody FitnessClass fitnessClass) {
        try {
            FitnessClass createdClass = fitnessClassService.createClass(fitnessClass);
            return new ResponseEntity<>(createdClass, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating class.");
        }
    }

    /**
     * Retrieves all classes (e.g., for admin view or full schedule).
     * Consider adding filters for day, type for public schedule view.
     * Corresponds to "Read: View class schedules and availability"
     * Endpoint: GET /api/classes
     * @return List of all classes.
     */
    @GetMapping
    public ResponseEntity<List<FitnessClass>> getAllClasses() {
        // For public schedule, you might want to return only active classes
        // List<FitnessClass> classes = fitnessClassService.getAllActiveClasses();
        List<FitnessClass> classes = fitnessClassService.getAllClasses(); // For now, return all
        return ResponseEntity.ok(classes);
    }

    /**
     * Retrieves all *active* classes (e.g., for public schedule page).
     * Endpoint: GET /api/classes/active
     * @return List of active classes.
     */
    @GetMapping("/active")
    public ResponseEntity<List<FitnessClass>> getActiveClasses() {
        List<FitnessClass> activeClasses = fitnessClassService.getAllActiveClasses();
        return ResponseEntity.ok(activeClasses);
    }


    /**
     * Retrieves a specific class by its ID.
     * Endpoint: GET /api/classes/{id}
     * @param id The ID of the class.
     * @return The class if found, or 404.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FitnessClass> getClassById(@PathVariable String id) {
        Optional<FitnessClass> fitnessClass = fitnessClassService.getClassById(id);
        return fitnessClass.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing fitness class (Admin operation).
     * Corresponds to "Update: Modify class timings or instructor"
     * Endpoint: PUT /api/classes/{id}
     * @param id The ID of the class to update.
     * @param classDetails Updated class data.
     * @return Updated class or 404/error.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClass(@PathVariable String id, @RequestBody FitnessClass classDetails) {
        try {
            Optional<FitnessClass> updatedClass = fitnessClassService.updateClass(id, classDetails);
            return updatedClass.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Class with ID " + id + " not found."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating class.");
        }
    }

    /**
     * Deletes a fitness class (Admin operation).
     * Corresponds to "Delete: Remove canceled classes"
     * Endpoint: DELETE /api/classes/{id}
     * @param id The ID of the class to delete.
     * @return No content if successful, or 404.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClass(@PathVariable String id) {
        boolean deleted = fitnessClassService.deleteClass(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Class with ID " + id + " not found.");
        }
    }
}
