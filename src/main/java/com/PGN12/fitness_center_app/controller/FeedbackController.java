package com.PGN12.fitness_center_app.controller;


import com.PGN12.fitness_center_app.model.Feedback;
import com.PGN12.fitness_center_app.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // For admin moderation payload
import java.util.Optional;

/**
 * REST Controller for managing Feedback and Reviews.
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Autowired
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * Endpoint for members to submit feedback.
     * Endpoint: POST /api/feedback
     * @param feedback The Feedback object from the request body.
     * @return ResponseEntity with the submitted feedback or an error.
     */
    @PostMapping
    public ResponseEntity<?> submitFeedback(@RequestBody Feedback feedback) {
        try {
            // Assuming memberId might be set by frontend if user is logged in and not anonymous
            // Or, it's explicitly set to null/empty for anonymous submissions.
            // The service layer will handle logic based on the 'anonymous' flag.
            Feedback submittedFeedback = feedbackService.submitFeedback(feedback);
            return new ResponseEntity<>(submittedFeedback, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error submitting feedback: " + e.getMessage());
        }
    }

    /**
     * Retrieves all APPROVED feedback/reviews for public display.
     * (Corresponds to "Read: Display reviews for trainers/classes")
     * Endpoint: GET /api/feedback/approved
     * @return List of approved feedback.
     */
    @GetMapping("/approved")
    public ResponseEntity<List<Feedback>> getApprovedFeedback(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String targetName) {
        List<Feedback> approvedFeedback = feedbackService.getApprovedFeedback();
        // Optional filtering
        if (type != null && !type.isEmpty()) {
            approvedFeedback = approvedFeedback.stream()
                    .filter(f -> type.equalsIgnoreCase(f.getFeedbackType()))
                    .collect(java.util.stream.Collectors.toList());
        }
        if (targetName != null && !targetName.isEmpty()) {
            approvedFeedback = approvedFeedback.stream()
                    .filter(f -> targetName.equalsIgnoreCase(f.getTargetName()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(approvedFeedback);
    }

    /**
     * Retrieves all feedback entries (Admin operation).
     * Can be filtered by status.
     * Endpoint: GET /api/feedback/admin/all
     * @param status Optional filter by status ("PENDING", "APPROVED", "REJECTED").
     * @return List of feedback.
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<Feedback>> getAllFeedbackForAdmin(@RequestParam(required = false) String status) {
        // In a real app, secure this endpoint for admins only
        List<Feedback> allFeedback = feedbackService.getAllFeedback();
        if (status != null && !status.isEmpty()) {
            allFeedback = allFeedback.stream()
                    .filter(f -> status.equalsIgnoreCase(f.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(allFeedback);
    }

    /**
     * Retrieves a specific feedback entry by ID.
     * Endpoint: GET /api/feedback/{feedbackId}
     * @param feedbackId The ID of the feedback.
     * @return Feedback if found, or 404.
     */
    @GetMapping("/{feedbackId}")
    public ResponseEntity<Feedback> getFeedbackById(@PathVariable String feedbackId) {
        return feedbackService.getFeedbackById(feedbackId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    /**
     * Allows a member to update their own feedback (limited fields).
     * Endpoint: PUT /api/feedback/{feedbackId}/member-edit
     * @param feedbackId The ID of the feedback to update.
     * @param feedbackDetails Updated details (e.g., comments, rating).
     * @param currentMemberId Needs to be passed, perhaps from a security context or request header in a real app.
     * For simplicity, we might expect it in the payload or as a header.
     * Here, let's assume it's part of the payload for demo.
     * @return Updated feedback or error.
     */
    @PutMapping("/{feedbackId}/member-edit")
    public ResponseEntity<?> updateMemberFeedback(@PathVariable String feedbackId,
                                                  @RequestBody Feedback feedbackDetails,
                                                  @RequestHeader(name = "X-Member-ID", required = false) String currentMemberIdHeader) {
        // In a real app, currentMemberId would come from authenticated principal
        String currentMemberId = feedbackDetails.getMemberId(); // Or from header: currentMemberIdHeader;
        if (currentMemberId == null || currentMemberId.trim().isEmpty()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Member ID is required to edit feedback.");
        }

        try {
            Feedback updated = feedbackService.updateFeedback(feedbackId, feedbackDetails, false, currentMemberId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating feedback.");
        }
    }


    /**
     * Allows an admin to moderate (approve/reject) feedback.
     * Endpoint: PUT /api/feedback/admin/moderate/{feedbackId}
     * @param feedbackId The ID of the feedback to moderate.
     * @param payload A Map containing "status" ("APPROVED" or "REJECTED") and optional "adminNotes".
     * @return Updated feedback or error.
     */
    @PutMapping("/admin/moderate/{feedbackId}")
    public ResponseEntity<?> moderateFeedback(@PathVariable String feedbackId, @RequestBody Map<String, String> payload) {
        // In a real app, secure this endpoint for admins only
        String newStatus = payload.get("status");
        String adminNotes = payload.get("adminNotes");

        if (newStatus == null || (!"APPROVED".equalsIgnoreCase(newStatus) && !"REJECTED".equalsIgnoreCase(newStatus))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status. Must be APPROVED or REJECTED.");
        }
        try {
            Feedback moderatedFeedback = feedbackService.moderateFeedback(feedbackId, newStatus.toUpperCase(), adminNotes);
            return ResponseEntity.ok(moderatedFeedback);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error moderating feedback.");
        }
    }

    /**
     * Deletes feedback (Admin operation).
     * Endpoint: DELETE /api/feedback/admin/{feedbackId}
     * @param feedbackId The ID of the feedback to delete.
     * @return No content if successful, or 404.
     */
    @DeleteMapping("/admin/{feedbackId}")
    public ResponseEntity<?> deleteFeedback(@PathVariable String feedbackId) {
        // In a real app, secure this endpoint for admins only
        boolean deleted = feedbackService.deleteFeedback(feedbackId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Feedback with ID " + feedbackId + " not found.");
        }
    }
}
