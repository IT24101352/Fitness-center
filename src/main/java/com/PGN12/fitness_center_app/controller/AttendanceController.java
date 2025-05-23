package com.PGN12.fitness_center_app.controller;

import com.PGN12.fitness_center_app.model.Attendance;
import com.PGN12.fitness_center_app.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for managing Attendance and Check-Ins.
 */
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Autowired
    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * Endpoint for member check-in, typically called by the kiosk.
     * Expects "memberId" and "checkInMethod" in the request body.
     * Optionally "classId".
     * Endpoint: POST /api/attendance/check-in
     * @param payload A Map containing memberId, checkInMethod, and optionally classId.
     * @return ResponseEntity with the created attendance record or an error.
     */
    @PostMapping("/check-in")
    public ResponseEntity<?> recordCheckIn(@RequestBody Map<String, String> payload) {
        String memberId = payload.get("memberId");
        String checkInMethod = payload.get("checkInMethod");
        String classId = payload.get("classId"); // This can be null or empty

        if (memberId == null || memberId.trim().isEmpty() ||
                checkInMethod == null || checkInMethod.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("memberId and checkInMethod are required.");
        }

        try {
            Attendance recordedAttendance = attendanceService.recordCheckIn(memberId, checkInMethod, Optional.ofNullable(classId));
            return new ResponseEntity<>(recordedAttendance, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during check-in: " + e.getMessage());
        }
    }

    /**
     * Retrieves all attendance records (Admin operation).
     * Endpoint: GET /api/attendance
     * @return List of all attendance records.
     */
    @GetMapping
    public ResponseEntity<List<Attendance>> getAllAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String memberId,
            @RequestParam(required = false) String classId
    ) {
        List<Attendance> records;
        if (startDate != null || endDate != null) {
            records = attendanceService.getAttendanceByDateRange(startDate, endDate);
        } else {
            records = attendanceService.getAllAttendanceRecords();
        }

        if (memberId != null && !memberId.isEmpty()) {
            records = records.stream().filter(att -> att.getMemberId().equals(memberId)).collect(Collectors.toList());
        }
        if (classId != null && !classId.isEmpty()) {
            records = records.stream().filter(att -> classId.equals(att.getClassId())).collect(Collectors.toList());
        }

        return ResponseEntity.ok(records);
    }

    /**
     * Retrieves a specific attendance record by its ID (Admin operation).
     * Endpoint: GET /api/attendance/{attendanceId}
     * @param attendanceId The ID of the attendance record.
     * @return The attendance record if found, or 404.
     */
    @GetMapping("/{attendanceId}")
    public ResponseEntity<Attendance> getAttendanceById(@PathVariable String attendanceId) {
        return attendanceService.getAllAttendanceRecords().stream() // Assuming findById in service
                .filter(att -> att.getAttendanceId().equals(attendanceId))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing attendance record (Admin operation).
     * Corresponds to "Update: Adjust check-in records if errors occur."
     * Endpoint: PUT /api/attendance/{attendanceId}
     * @param attendanceId The ID of the record to update.
     * @param attendanceDetails The updated attendance data.
     * @return The updated record or 404/error.
     */
    @PutMapping("/{attendanceId}")
    public ResponseEntity<?> updateAttendance(@PathVariable String attendanceId, @RequestBody Attendance attendanceDetails) {
        try {
            Optional<Attendance> updatedRecord = attendanceService.updateAttendanceRecord(attendanceId, attendanceDetails);
            return updatedRecord.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Attendance record " + attendanceId + " not found."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating attendance record.");
        }
    }

    /**
     * Deletes an attendance record (Admin operation).
     * Corresponds to "Delete: Remove duplicate/invalid entries."
     * Endpoint: DELETE /api/attendance/{attendanceId}
     * @param attendanceId The ID of the record to delete.
     * @return No content if successful, or 404.
     */
    @DeleteMapping("/{attendanceId}")
    public ResponseEntity<?> deleteAttendance(@PathVariable String attendanceId) {
        boolean deleted = attendanceService.deleteAttendanceRecord(attendanceId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Attendance record " + attendanceId + " not found.");
        }
    }
}
