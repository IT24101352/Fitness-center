package com.PGN12.fitness_center_app.repository;

import com.PGN12.fitness_center_app.model.Attendance;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for Attendance data, handles file-based storage in attendance.txt.
 */
@Repository
public class AttendanceRepository {

    private static final String FILE_NAME = "attendance.txt";
    private final Path filePath;
    private static final String DELIMITER = "|";

    public AttendanceRepository() {
        Path dataDir = Paths.get("src", "main", "resources", "data");
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                System.err.println("Error creating data directory for attendance: " + e.getMessage());
            }
        }
        this.filePath = dataDir.resolve(FILE_NAME);
        if (!Files.exists(this.filePath)) {
            try {
                Files.createFile(this.filePath);
                System.out.println("Attendance data file created: " + this.filePath.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error creating attendance data file: " + this.filePath.toAbsolutePath() + " - " + e.getMessage());
            }
        }
        System.out.println("AttendanceRepository initialized. Data file path: " + this.filePath.toAbsolutePath());
    }

    private String attendanceToString(Attendance attendance) {
        return String.join(DELIMITER,
                escape(attendance.getAttendanceId()),
                escape(attendance.getMemberId()),
                escape(attendance.getMemberName()),
                escape(attendance.getCheckInTimestamp()),
                escape(attendance.getCheckOutTimestamp() == null ? "" : attendance.getCheckOutTimestamp()), // Handle null checkout
                escape(attendance.getCheckInMethod()),
                escape(attendance.getClassId() == null ? "" : attendance.getClassId()), // Handle null classId
                escape(attendance.getClassName() == null ? "" : attendance.getClassName()) // Handle null className
        );
    }

    private Attendance stringToAttendance(String line) {
        String[] parts = line.split("\\" + DELIMITER, -1);
        if (parts.length >= 8) { // Adjusted for new fields
            Attendance attendance = new Attendance();
            attendance.setAttendanceId(unescape(parts[0]));
            attendance.setMemberId(unescape(parts[1]));
            attendance.setMemberName(unescape(parts[2]));
            attendance.setCheckInTimestamp(unescape(parts[3]));
            attendance.setCheckOutTimestamp(parts[4].isEmpty() ? null : unescape(parts[4]));
            attendance.setCheckInMethod(unescape(parts[5]));
            attendance.setClassId(parts[6].isEmpty() ? null : unescape(parts[6]));
            attendance.setClassName(parts[7].isEmpty() ? null : unescape(parts[7]));
            return attendance;
        }
        System.err.println("Skipping malformed line in " + FILE_NAME + ": " + line + " (Expected 8 parts, got " + parts.length + ")");
        return null;
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace(DELIMITER, "\\" + DELIMITER).replace("\n", "\\n");
    }

    private String unescape(String value) {
        if (value == null) return "";
        return value.replace("\\n", "\n").replace("\\" + DELIMITER, DELIMITER);
    }

    public List<Attendance> findAll() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.lines()
                    .map(this::stringToAttendance)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading from " + FILE_NAME + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<Attendance> findById(String attendanceId) {
        return findAll().stream()
                .filter(att -> att.getAttendanceId().equals(attendanceId))
                .findFirst();
    }

    public Attendance save(Attendance attendance) {
        List<Attendance> attendances = findAll();
        boolean updated = false;
        for (int i = 0; i < attendances.size(); i++) {
            if (attendances.get(i).getAttendanceId().equals(attendance.getAttendanceId())) {
                attendances.set(i, attendance); // Update existing
                updated = true;
                break;
            }
        }
        if (!updated) {
            attendances.add(attendance); // Add new
        }
        writeToFile(attendances);
        return attendance;
    }

    /**
     * Deletes an attendance record by its ID.
     * (Corresponds to "Delete: Remove duplicate/invalid entries" from project distribution)
     * @param attendanceId The ID of the attendance record to delete.
     * @return true if deleted, false otherwise.
     */
    public boolean deleteById(String attendanceId) {
        List<Attendance> attendances = findAll();
        boolean removed = attendances.removeIf(att -> att.getAttendanceId().equals(attendanceId));
        if (removed) {
            writeToFile(attendances);
        }
        return removed;
    }


    private void writeToFile(List<Attendance> attendances) {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Attendance att : attendances) {
                writer.write(attendanceToString(att));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to " + FILE_NAME + ": " + e.getMessage());
        }
    }
}