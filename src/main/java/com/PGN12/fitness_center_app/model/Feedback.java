package com.PGN12.fitness_center_app.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents member feedback or a review.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    private String feedbackId;    // Unique ID (e.g., "FDBK001")
    private String memberId;      // ID of the member submitting (null if anonymous and allowed)
    private String reviewerName;  // Name of the reviewer (could be "Anonymous" or actual name)
    private boolean anonymous;    // True if submitted anonymously

    private String feedbackType;  // e.g., "general", "class", "trainer", "facility", "suggestion"
    private String targetName;    // Name of the class, trainer, or specific facility item (e.g., "Sunrise Yoga", "Mike T.", "Locker Room Showers")
    // private String targetId;   // Optional: If you have specific IDs for classes/trainers being reviewed

    private Integer rating;       // Optional: 1-5 stars (Integer to allow null)
    private String comments;      // The actual feedback text
    private String submissionDate; // ISO_LOCAL_DATE_TIME format string

    private String status;        // For admin moderation: "PENDING", "APPROVED", "REJECTED"
    private String adminNotes;    // Optional notes from admin during moderation
    private boolean verifiedReviewer; // Could be set if the memberId is present and member is verified

    // OOP Concepts:
    // Encapsulation: Handled by Lombok's @Data.
    // Inheritance (AnonymousFeedback, VerifiedFeedback):
    // For simplicity with file-based storage, we use the 'anonymous' boolean field
    // and 'verifiedReviewer' boolean. If complex distinct behaviors were needed,
    // separate classes inheriting from a base Feedback class would be an option,
    // but it complicates file parsing significantly.
}
