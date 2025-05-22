package com.PGN12.fitness_center_app.service;

import com.PGN12.fitness_center_app.model.Feedback;
import com.PGN12.fitness_center_app.model.Member; // If verifying member
import com.PGN12.fitness_center_app.repository.FeedbackRepository;
import com.PGN12.fitness_center_app.repository.MemberRepository; // If verifying member
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for Feedback and Review management.
 */
@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository; // Optional: for verifying member

    @Autowired
    public FeedbackService(FeedbackRepository feedbackRepository, MemberRepository memberRepository) {
        this.feedbackRepository = feedbackRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Submits new feedback.
     * (Corresponds to "Create: Submit feedback stored in feedback.txt")
     * @param feedback The feedback object to submit.
     * @return The saved Feedback object.
     */
    public Feedback submitFeedback(Feedback feedback) {
        if (feedback.getComments() == null || feedback.getComments().trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback comments cannot be empty.");
        }
        if (feedback.getFeedbackType() == null || feedback.getFeedbackType().trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback type is required.");
        }

        feedback.setFeedbackId("FDBK" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        feedback.setSubmissionDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        feedback.setStatus("PENDING"); // New feedback defaults to pending moderation

        if (feedback.isAnonymous()) {
            feedback.setMemberId(null); // Ensure memberId is null if anonymous
            feedback.setReviewerName("Anonymous");
            feedback.setVerifiedReviewer(false);
        } else {
            if (feedback.getMemberId() != null) {
                Optional<Member> memberOpt = memberRepository.findById(feedback.getMemberId());
                if (memberOpt.isPresent()) {
                    feedback.setReviewerName(memberOpt.get().getFullName()); // Set reviewer name from member profile
                    feedback.setVerifiedReviewer(true); // Assuming member ID implies verification
                } else {
                    // Member ID provided but not found, treat as unverified or throw error
                    feedback.setReviewerName(feedback.getReviewerName() != null ? feedback.getReviewerName() : "Unknown User");
                    feedback.setVerifiedReviewer(false);
                    // Or throw new IllegalArgumentException("Member ID " + feedback.getMemberId() + " not found for non-anonymous feedback.");
                }
            } else if (feedback.getReviewerName() == null || feedback.getReviewerName().trim().isEmpty()){
                throw new IllegalArgumentException("Reviewer name or Member ID is required for non-anonymous feedback.");
            }
        }


        return feedbackRepository.save(feedback);
    }

    /**
     * Retrieves all feedback entries (primarily for admin).
     * @return List of all feedback.
     */
    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

    /**
     * Retrieves feedback entries that are approved (for public display).
     * (Corresponds to "Read: Display reviews for trainers/classes")
     * @return List of approved feedback.
     */
    public List<Feedback> getApprovedFeedback() {
        return feedbackRepository.findAll().stream()
                .filter(f -> "APPROVED".equalsIgnoreCase(f.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves feedback by its ID.
     * @param feedbackId The ID of the feedback.
     * @return Optional containing the feedback if found.
     */
    public Optional<Feedback> getFeedbackById(String feedbackId) {
        return feedbackRepository.findById(feedbackId);
    }


    /**
     * Updates feedback (e.g., member editing their own feedback, or admin moderating).
     * (Corresponds to "Update: Allow members to edit feedback" and admin moderation)
     * @param feedbackId The ID of the feedback to update.
     * @param updatedFeedbackDetails The Feedback object with new details.
     * @param isAdminOp Boolean indicating if this is an admin operation (allows status change).
     * @param currentMemberId Optional: ID of the member attempting to edit (for ownership check).
     * @return The updated Feedback object.
     * @throws IllegalAccessException if a non-admin tries to edit someone else's feedback or change status.
     * @throws IllegalArgumentException if feedback not found.
     */
    public Feedback updateFeedback(String feedbackId, Feedback updatedFeedbackDetails, boolean isAdminOp, String currentMemberId) throws IllegalAccessException {
        Optional<Feedback> existingFeedbackOpt = feedbackRepository.findById(feedbackId);
        if (existingFeedbackOpt.isEmpty()) {
            throw new IllegalArgumentException("Feedback with ID " + feedbackId + " not found.");
        }
        Feedback existingFeedback = existingFeedbackOpt.get();

        if (!isAdminOp) { // Member editing their own feedback
            if (existingFeedback.isAnonymous() || !existingFeedback.getMemberId().equals(currentMemberId)) {
                throw new IllegalAccessException("You are not authorized to edit this feedback.");
            }
            if (updatedFeedbackDetails.getStatus() != null && !updatedFeedbackDetails.getStatus().equals(existingFeedback.getStatus())) {
                throw new IllegalAccessException("Members cannot change feedback status.");
            }
            // Members can only update comments and rating, perhaps targetName/type if submitted recently
            if (updatedFeedbackDetails.getComments() != null) existingFeedback.setComments(updatedFeedbackDetails.getComments());
            if (updatedFeedbackDetails.getRating() != null) existingFeedback.setRating(updatedFeedbackDetails.getRating());
            // Potentially re-validate reviewer name if anonymous status changes, though not typical for edits
        } else { // Admin operation
            if (updatedFeedbackDetails.getStatus() != null) existingFeedback.setStatus(updatedFeedbackDetails.getStatus());
            if (updatedFeedbackDetails.getAdminNotes() != null) existingFeedback.setAdminNotes(updatedFeedbackDetails.getAdminNotes());
            if (updatedFeedbackDetails.getComments() != null) existingFeedback.setComments(updatedFeedbackDetails.getComments()); // Admin might correct typos
            if (updatedFeedbackDetails.getRating() != null) existingFeedback.setRating(updatedFeedbackDetails.getRating());
            if (updatedFeedbackDetails.getTargetName() != null) existingFeedback.setTargetName(updatedFeedbackDetails.getTargetName());
            if (updatedFeedbackDetails.getFeedbackType() != null) existingFeedback.setFeedbackType(updatedFeedbackDetails.getFeedbackType());
        }
        existingFeedback.setSubmissionDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); // Update submission date on edit

        return feedbackRepository.save(existingFeedback);
    }

    /**
     * Updates only the status and admin notes of a feedback entry (Admin operation).
     * @param feedbackId The ID of the feedback.
     * @param newStatus The new status ("APPROVED", "REJECTED").
     * @param adminNotes Optional notes from the admin.
     * @return The updated Feedback object.
     */
    public Feedback moderateFeedback(String feedbackId, String newStatus, String adminNotes) {
        Optional<Feedback> feedbackOpt = feedbackRepository.findById(feedbackId);
        if (feedbackOpt.isEmpty()) {
            throw new IllegalArgumentException("Feedback with ID " + feedbackId + " not found.");
        }
        Feedback feedback = feedbackOpt.get();
        feedback.setStatus(newStatus);
        if (adminNotes != null) {
            feedback.setAdminNotes(adminNotes);
        }
        feedback.setSubmissionDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); // Update timestamp
        return feedbackRepository.save(feedback);
    }


    /**
     * Deletes feedback (Admin operation).
     * (Corresponds to "Delete: Remove inappropriate reviews")
     * @param feedbackId The ID of the feedback to delete.
     * @return true if deleted, false otherwise.
     */
    public boolean deleteFeedback(String feedbackId) {
        return feedbackRepository.deleteById(feedbackId);
    }
}
