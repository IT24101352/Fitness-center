package com.PGN12.fitness_center_app.model;



import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List; // For features

/**
 * Represents a membership plan offered by the fitness center.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlan {

    private String id; // Unique identifier for the plan (e.g., "PLAN001")
    private String name; // e.g., "Basic Access", "Premium Plus"
    private double price; // Price per month
    private String duration; // e.g., "Monthly", "Yearly", "3 Months"
    private String description; // A comma-separated string of features for simplicity with file storage
    // In a real DB, this might be a List<String> or a separate table.
    private String status; // e.g., "active", "inactive"

    // Note for OOP Inheritance:
    // As per your project distribution, you could have:
    // public class TrialPlan extends MembershipPlan { /* Trial specific attributes */ }
    // public class YearlyPlan extends MembershipPlan { /* Yearly specific attributes */ }
    // For now, we'll use a single MembershipPlan class.
}
