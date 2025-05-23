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
 * Handles persistence operations for Booking data using file-based storage.
 * Implements CRUD operations and custom queries for booking management.
 */
@Repository
public class BookingRepository {

    // Configuration constants
    private static final String FILE_NAME = "bookings.txt";
    private static final String DELIMITER = "|"; // Field separator in storage file
    private final Path filePath; // Path to the data file

    /**
     * Initializes the repository by setting up required data directory and file.
     * Creates them if they don't exist.
     */
    public BookingRepository() {
        // Set up data directory path under src/main/resources/data
        Path dataDir = Paths.get("src", "main", "resources", "data");

        try {
            // Create directory if it doesn't exist
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }

            // Initialize the bookings file path
            this.filePath = dataDir.resolve(FILE_NAME);

            // Create bookings file if it doesn't exist
            if (!Files.exists(this.filePath)) {
                Files.createFile(this.filePath);
                System.out.println("Created new bookings file: " + this.filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize booking repository storage", e);
        }
    }

    // ================== CRUD OPERATIONS ================== //

    /**
     * Saves a booking to the repository (create or update)
     * @param booking The booking to save
     * @return The saved booking
     */
    public Booking save(Booking booking) {
        // 1. Read all existing bookings
        List<Booking> bookings = findAll();

        // 2. Check if booking exists
        boolean exists = false;
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).getBookingId().equals(booking.getBookingId())) {
                // 3a. Update existing booking
                bookings.set(i, booking);
                exists = true;
                break;
            }
        }

        // 3b. Add new booking if it didn't exist
        if (!exists) {
            bookings.add(booking);
        }

        // 4. Persist all bookings to file
        writeToFile(bookings);
        return booking;
    }

    /**
     * Retrieves all bookings from the repository
     * @return List of all bookings
     */
    public List<Booking> findAll() {
        // Return empty list if file doesn't exist yet
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            // Read all lines and convert to Booking objects
            return reader.lines()
                    .map(this::stringToBooking)
                    .filter(booking -> booking != null) // Skip malformed entries
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bookings from storage", e);
        }
    }

    /**
     * Finds a booking by its ID
     * @param bookingId The ID to search for
     * @return Optional containing the booking if found
     */
    public Optional<Booking> findById(String bookingId) {
        return findAll().stream()
                .filter(booking -> booking.getBookingId().equals(bookingId))
                .findFirst();
    }

    /**
     * Finds all bookings for a specific member
     * @param memberId The member ID to filter by
     * @return List of matching bookings
     */
    public List<Booking> findByMemberId(String memberId) {
        return findAll().stream()
                .filter(booking -> booking.getMemberId().equals(memberId))
                .collect(Collectors.toList());
    }

    /**
     * Finds all bookings for a specific fitness class
     * @param classId The class ID to filter by
     * @return List of matching bookings
     */
    public List<Booking> findByClassId(String classId) {
        return findAll().stream()
                .filter(booking -> booking.getClassId().equals(classId))
                .collect(Collectors.toList());
    }

    /**
     * Deletes a booking by its ID
     * @param bookingId The ID of the booking to delete
     * @return true if booking was found and deleted, false otherwise
     */
    public boolean deleteById(String bookingId) {
        List<Booking> bookings = findAll();

        // Remove booking if exists
        boolean wasRemoved = bookings.removeIf(
                booking -> booking.getBookingId().equals(bookingId)
        );

        if (wasRemoved) {
            // Update storage if booking was deleted
            writeToFile(bookings);
        }

        return wasRemoved;
    }

    // ================== HELPER METHODS ================== //

    /**
     * Writes all bookings to the data file (overwrites existing content)
     * @param bookings List of bookings to persist
     */
    private void writeToFile(List<Booking> bookings) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            // Write each booking as a new line
            for (Booking booking : bookings) {
                writer.write(bookingToString(booking));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist bookings to storage", e);
        }
    }

    /**
     * Converts Booking object to storage string format
     * @param booking The booking to convert
     * @return Pipe-delimited string representation
     */
    private String bookingToString(Booking booking) {
        return String.join(DELIMITER,
                escape(booking.getBookingId()),
                escape(booking.getMemberId()),
                escape(booking.getClassId()),
                escape(booking.getBookingTimestamp()),
                escape(booking.getStatus())
        );
    }

    /**
     * Parses a string line into a Booking object
     * @param line The input line from storage file
     * @return Booking object or null if malformed
     */
    private Booking stringToBooking(String line) {
        String[] parts = line.split("\\" + DELIMITER, -1);
        if (parts.length == 5) { // Verify expected field count
            Booking booking = new Booking();
            booking.setBookingId(unescape(parts[0]));
            booking.setMemberId(unescape(parts[1]));
            booking.setClassId(unescape(parts[2]));
            booking.setBookingTimestamp(unescape(parts[3]));
            booking.setStatus(unescape(parts[4]));
            return booking;
        }
        System.err.println("Skipping malformed booking record: " + line);
        return null;
    }

    /**
     * Escapes special characters for storage
     * @param value The string to escape
     * @return Escaped string
     */
    private String escape(String value) {
        if (value == null) return "";
        return value.replace(DELIMITER, "\\" + DELIMITER)
                .replace("\n", "\\n");
    }

    /**
     * Unescapes stored values back to original form
     * @param value The string to unescape
     * @return Original string
     */
    private String unescape(String value) {
        if (value == null) return "";
        return value.replace("\\n", "\n")
                .replace("\\" + DELIMITER, DELIMITER);
    }
}