package com.PGN12.fitness_center_app.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a member's attendance record (check-in).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    private String attendanceId; // Unique ID for this attendance record (e.g., ATT001)
    private String memberId;
    private String memberName; // Denormalized for easier display in reports
    private String checkInTimestamp; // ISO_LOCAL_DATE_TIME format string
    private String checkOutTimestamp; // Optional: ISO_LOCAL_DATE_TIME format string, can be null if not implemented
    private String checkInMethod; // e.g., "MANUAL_ID", "QR_SCAN" (from k.html)
    private String classId; // Optional: ID of the class if check-in is for a specific class
    private String className; // Optional: Name of the class, denormalized

    // Note: Your k.html has a "class" field in the mock data for MEM004.
    // We'll assume a check-in can be general or for a specific class.
}