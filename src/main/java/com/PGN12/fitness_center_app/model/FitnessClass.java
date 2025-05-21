package com.PGN12.fitness_center_app.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a fitness class offered at the center.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FitnessClass {

    private String id; // Unique ID for the class (e.g., "CLS001")
    private String name; // e.g., "Sunrise Yoga", "HIIT Blast"
    private String instructor;
    private String dayOfWeek; // e.g., "monday", "tuesday" (consistent with your admin_class_management.html filter)
    private String time; // Store as HH:mm (24-hour format for consistency, can be formatted for display)
    private String duration; // e.g., "60 min", "45 min"
    private int capacity; // Maximum number of spots
    private int bookedSpots; // Number of currently booked spots
    private String type; // e.g., "yoga", "hiit", "cycling"
    private String description;
    private String status; // e.g., "active", "cancelled" (from admin_class_management.html)

    // Helper method to check if class has available spots
    public boolean hasAvailableSpots() {
        return bookedSpots < capacity;
    }

    // Helper method to increment booked spots
    public boolean incrementBookedSpots() {
        if (hasAvailableSpots()) {
            this.bookedSpots++;
            return true;
        }
        return false;
    }

    // Helper method to decrement booked spots
    public void decrementBookedSpots() {
        if (this.bookedSpots > 0) {
            this.bookedSpots--;
        }
    }
}
