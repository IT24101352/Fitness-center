package com.PGN12.fitness_center_app.service;

import com.PGN12.fitness_center_app.model.Attendance;
import com.PGN12.fitness_center_app.model.Member;
import com.PGN12.fitness_center_app.model.FitnessClass; // If linking to specific class
import com.PGN12.fitness_center_app.repository.AttendanceRepository;
import com.PGN12.fitness_center_app.repository.MemberRepository;
import com.PGN12.fitness_center_app.repository.FitnessClassRepository; // If linking
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for Attendance and Check-In logic.
 */
@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final FitnessClassRepository fitnessClassRepository; // Optional, if check-ins link to classes

    @Autowired
    public AttendanceService(AttendanceRepository attendanceRepository,
                             MemberRepository memberRepository,
                             FitnessClassRepository fitnessClassRepository) {
        this.attendanceRepository = attendanceRepository;
        this.memberRepository = memberRepository;
        this.fitnessClassRepository = fitnessClassRepository;
    }

    /**
     * Logs a member check-in.
     * (Corresponds to "Create: Log member check-ins in attendance.txt")
     * @param memberId The ID of the member checking in.
     * @param checkInMethod The method used for check-in (e.g., "MANUAL_ID", "QR_SCAN").
     * @param classId Optional ID of the class the member is checking in for.
     * @return The created Attendance record.
     * @throws IllegalArgumentException if member is not found or invalid.
     */
    public Attendance recordCheckIn(String memberId, String checkInMethod, Optional<String> classIdOpt) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("Member with ID " + memberId + " not found. Cannot record check-in.");
        }
        Member member = memberOpt.get();

        // Basic validation for member status if you have it in Member model
        // if ("inactive".equalsIgnoreCase(member.getStatus())) {
        //     throw new IllegalArgumentException("Member " + member.getFullName() + " is inactive. Check-in denied.");
        // }

        Attendance attendance = new Attendance();
        attendance.setAttendanceId("ATT" + UUID.randomUUID().toString().substring(0, 7).toUpperCase());
        attendance.setMemberId(member.getId());
        attendance.setMemberName(member.getFullName()); // Denormalize for easier reporting
        attendance.setCheckInTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        attendance.setCheckInMethod(checkInMethod);
        // attendance.setCheckOutTimestamp(null); // Initially null

        if (classIdOpt.isPresent() && !classIdOpt.get().isBlank()) {
            String actualClassId = classIdOpt.get();
            Optional<FitnessClass> classOpt = fitnessClassRepository.findById(actualClassId);
            if (classOpt.isPresent()) {
                attendance.setClassId(actualClassId);
                attendance.setClassName(classOpt.get().getName());
            } else {
                System.err.println("Warning: Class ID " + actualClassId + " provided for check-in but not found. Storing ID only.");
                attendance.setClassId(actualClassId); // Store ID even if class details not found
                attendance.setClassName("Unknown Class (ID: " + actualClassId + ")");
            }
        }

        return attendanceRepository.save(attendance);
    }

    /**
     * Retrieves all attendance records.
     * (Corresponds to "Read: View attendance reports")
     * @return A list of all attendance records.
     */
    public List<Attendance> getAllAttendanceRecords() {
        return attendanceRepository.findAll();
    }

    /**
     * Retrieves attendance records for a specific member.
     * @param memberId The ID of the member.
     * @return List of attendance records for that member.
     */
    public List<Attendance> getAttendanceByMemberId(String memberId) {
        return attendanceRepository.findAll().stream()
                .filter(att -> att.getMemberId().equals(memberId))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves attendance records for a specific class.
     * @param classId The ID of the class.
     * @return List of attendance records for that class.
     */
    public List<Attendance> getAttendanceByClassId(String classId) {
        return attendanceRepository.findAll().stream()
                .filter(att -> classId.equals(att.getClassId()))
                .collect(Collectors.toList());
    }

    /**
     * Updates an attendance record.
     * (Corresponds to "Update: Adjust check-in records if errors occur")
     * This is a basic update, more complex logic might be needed.
     * @param attendanceId The ID of the attendance record to update.
     * @param detailsToUpdate The Attendance object with new details.
     * @return The updated Attendance record, or Optional.empty() if not found.
     */
    public Optional<Attendance> updateAttendanceRecord(String attendanceId, Attendance detailsToUpdate) {
        Optional<Attendance> existingOpt = attendanceRepository.findById(attendanceId);
        if (existingOpt.isPresent()) {
            Attendance existing = existingOpt.get();
            // Update relevant fields - be careful what you allow to be updated
            if (detailsToUpdate.getCheckOutTimestamp() != null) {
                existing.setCheckOutTimestamp(detailsToUpdate.getCheckOutTimestamp());
            }
            if (detailsToUpdate.getCheckInMethod() != null) {
                existing.setCheckInMethod(detailsToUpdate.getCheckInMethod());
            }
            // Potentially update classId, className if a mistake was made.
            // MemberId and checkInTimestamp usually shouldn't be changed post-creation.
            return Optional.of(attendanceRepository.save(existing));
        }
        return Optional.empty();
    }

    /**
     * Deletes an attendance record.
     * (Corresponds to "Delete: Remove duplicate/invalid entries")
     * @param attendanceId The ID of the record to delete.
     * @return true if deleted, false otherwise.
     */
    public boolean deleteAttendanceRecord(String attendanceId) {
        return attendanceRepository.deleteById(attendanceId);
    }

    // Add methods for filtering by date range, etc., for reports if needed.
    public List<Attendance> getAttendanceByDateRange(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return attendanceRepository.findAll().stream()
                .filter(att -> {
                    try {
                        LocalDateTime checkInTime = LocalDateTime.parse(att.getCheckInTimestamp(), formatter);
                        boolean afterStart = start == null || !checkInTime.isBefore(start);
                        boolean beforeEnd = end == null || !checkInTime.isAfter(end);
                        return afterStart && beforeEnd;
                    } catch (Exception e) {
                        System.err.println("Error parsing date for attendance record " + att.getAttendanceId() + ": " + att.getCheckInTimestamp());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }
}
