package com.PGN12.fitness_center_app.repository;


import com.PGN12.fitness_center_app.model.Booking;
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
 * Repository for Booking data, handles file-based storage in bookings.txt.
 */
@Repository
public class BookingRepository {

    private static final String FILE_NAME = "bookings.txt";
    private final Path filePath;
    private static final String DELIMITER = "|";

    public BookingRepository() {
        Path dataDir = Paths.get("src", "main", "resources", "data");
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                System.err.println("Error creating data directory for bookings: " + e.getMessage());
            }
        }
        this.filePath = dataDir.resolve(FILE_NAME);
        if (!Files.exists(this.filePath)) {
            try {
                Files.createFile(this.filePath);
                System.out.println("Bookings data file created: " + this.filePath.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error creating bookings data file: " + this.filePath.toAbsolutePath() + " - " + e.getMessage());
            }
        }
        System.out.println("BookingRepository initialized. Data file path: " + this.filePath.toAbsolutePath());
    }

    private String bookingToString(Booking booking) {
        return String.join(DELIMITER,
                escape(booking.getBookingId()),
                escape(booking.getMemberId()),
                escape(booking.getClassId()),
                escape(booking.getBookingTimestamp()),
                escape(booking.getStatus())
        );
    }

    private Booking stringToBooking(String line) {
        String[] parts = line.split("\\" + DELIMITER, -1);
        if (parts.length >= 5) {
            Booking booking = new Booking();
            booking.setBookingId(unescape(parts[0]));
            booking.setMemberId(unescape(parts[1]));
            booking.setClassId(unescape(parts[2]));
            booking.setBookingTimestamp(unescape(parts[3]));
            booking.setStatus(unescape(parts[4]));
            return booking;
        }
        System.err.println("Skipping malformed line in " + FILE_NAME + ": " + line);
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

    public List<Booking> findAll() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.lines()
                    .map(this::stringToBooking)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading from " + FILE_NAME + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<Booking> findById(String bookingId) {
        return findAll().stream()
                .filter(b -> b.getBookingId().equals(bookingId))
                .findFirst();
    }

    public List<Booking> findByMemberId(String memberId) {
        return findAll().stream()
                .filter(b -> b.getMemberId().equals(memberId))
                .collect(Collectors.toList());
    }

    public List<Booking> findByClassId(String classId) {
        return findAll().stream()
                .filter(b -> b.getClassId().equals(classId))
                .collect(Collectors.toList());
    }

    public Booking save(Booking booking) {
        List<Booking> bookings = findAll();
        boolean updated = false;
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).getBookingId().equals(booking.getBookingId())) {
                bookings.set(i, booking);
                updated = true;
                break;
            }
        }
        if (!updated) {
            bookings.add(booking);
        }
        writeToFile(bookings);
        return booking;
    }

    public boolean deleteById(String bookingId) {
        List<Booking> bookings = findAll();
        boolean removed = bookings.removeIf(b -> b.getBookingId().equals(bookingId));
        if (removed) {
            writeToFile(bookings);
        }
        return removed;
    }

    private void writeToFile(List<Booking> bookings) {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Booking b : bookings) {
                writer.write(bookingToString(b));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
