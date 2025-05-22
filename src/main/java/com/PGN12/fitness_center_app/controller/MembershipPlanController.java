package com.PGN12.fitness_center_app.controller;




import com.PGN12.fitness_center_app.model.MembershipPlan;
import com.PGN12.fitness_center_app.service.MembershipPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for managing Membership Plans.
 */
@RestController //Tells Spring that this class handles REST API requests
@RequestMapping("/api/plans") // Base path for plan-related APIs so every method path starts with /api/plans
public class MembershipPlanController {

    private final MembershipPlanService planService;

    @Autowired //The controller hands off the core processing tasks to this service.
    public MembershipPlanController(MembershipPlanService planService) {

        this.planService = planService;
    }

    /**
     * Creates a new membership plan.
     * Corresponds to "Create: Add new membership plans" from project distribution.
     * Endpoint: POST /api/plans
     * @param plan The plan data from the request body.
     * @return ResponseEntity with the created plan or an error message.
     */
    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody MembershipPlan plan) { // Changed to ResponseEntity<?>
        // exposes only the necessary functionality to the user (abstraction)
        try {
            MembershipPlan createdPlan = planService.createPlan(plan);
            return new ResponseEntity<>(createdPlan, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            // Return a String body for bad requests (client errors)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Log the exception for server errors
            e.printStackTrace(); // Good practice to log the full stack trace
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating plan.");
        }
    }

    /**
     * Retrieves all membership plans.
     * Corresponds to "Read: Display available plans and pricing".
     * Endpoint: GET /api/plans
     * @return A list of all plans.
     */
    @GetMapping
    public ResponseEntity<List<MembershipPlan>> getAllPlans() {
        List<MembershipPlan> plans = planService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    /**
     * Retrieves a specific plan by its ID.
     * Endpoint: GET /api/plans/{id}
     * @param id The ID of the plan.
     * @return The plan if found, or 404 Not Found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MembershipPlan> getPlanById(@PathVariable String id) {
        Optional<MembershipPlan> plan = planService.getPlanById(id);
        return plan.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing membership plan.
     * Corresponds to "Update: Modify plan details".
     * Endpoint: PUT /api/plans/{id}
     * @param id The ID of the plan to update.
     * @param planDetails The updated plan data.
     * @return The updated plan or 404 if not found, or error.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable String id, @RequestBody MembershipPlan planDetails) { // Changed to ResponseEntity<?>
        try {
            Optional<MembershipPlan> updatedPlanOpt = planService.updatePlan(id, planDetails);
            // Use .map to handle the Optional and convert to ResponseEntity<MembershipPlan> or ResponseEntity<String>
            return updatedPlanOpt
                    .<ResponseEntity<?>>map(plan -> ResponseEntity.ok(plan)) // If present, wrap MembershipPlan in ResponseEntity
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Plan with ID " + id + " not found.")); // If not present, return 404 with String body
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating plan.");
        }
    }

    /**
     * Deletes a membership plan by its ID.
     * Corresponds to "Delete: Remove outdated plans".
     * Endpoint: DELETE /api/plans/{id}
     * @param id The ID of the plan to delete.
     * @return ResponseEntity with no content if successful, or 404 if not found.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlan(@PathVariable String id) { // Changed to ResponseEntity<?>
        boolean deleted = planService.deletePlan(id);
        if (deleted) {
            return ResponseEntity.noContent().build(); // HTTP 204 No Content
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Plan with ID " + id + " not found.");
        }
    }
}
// ResponseEntity is a class in Spring Framework
// used in Spring REST APIs to represent the entire HTTP response