package com.PGN12.fitness_center_app.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a member's booking for a fitness class.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    private String bookingId; // Unique ID for the booking (e.g., "BOOK001")
    private String memberId; // ID of the member who booked
    private String classId; // ID of the class booked
    private String bookingTimestamp; // When the booking was made (ISO_LOCAL_DATE_TIME format)
    private String status; // e.g., "CONFIRMED", "CANCELLED_MEMBER", "CANCELLED_ADMIN"

    // Optional: Add class details if you want to denormalize for easier display of "my bookings"
    // private String className;
    // private String classDay;
    // private String classTime;
}

