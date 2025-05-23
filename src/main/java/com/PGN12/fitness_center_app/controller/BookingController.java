package com.PGN12.fitness_center_app.controller;

import com.PGN12.fitness_center_app.model.Booking;
import com.PGN12.fitness_center_app.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // For request body if needed

/**
 * REST Controller for managing Bookings.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Creates a new booking for a class.
     * Expects memberId and classId in the request body.
     * Endpoint: POST /api/bookings
     * @param payload A Map containing "memberId" and "classId".
     * @return ResponseEntity with the created booking or an error.
     */
    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody Map<String, String> payload) {
        String memberId = payload.get("memberId");
        String classId = payload.get("classId");

        if (memberId == null || classId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("memberId and classId are required.");
        }

        try {
            Booking newBooking = bookingService.createBooking(memberId, classId);
            return new ResponseEntity<>(newBooking, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) { // Catch potential runtime issues like concurrent booking
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while creating the booking.");
        }
    }

    /**
     * Retrieves all bookings for a specific member.
     * Endpoint: GET /api/bookings/member/{memberId}
     * @param memberId The ID of the member.
     * @return List of bookings for the member.
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<Booking>> getBookingsByMember(@PathVariable String memberId) {
        List<Booking> bookings = bookingService.getBookingsByMemberId(memberId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Retrieves a specific booking by its ID.
     * Endpoint: GET /api/bookings/{bookingId}
     * @param bookingId The ID of the booking.
     * @return The booking if found, or 404.
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBookingById(@PathVariable String bookingId) {
        return bookingService.getBookingById(bookingId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Cancels a booking.
     * Endpoint: PUT /api/bookings/{bookingId}/cancel
     * @param bookingId The ID of the booking to cancel.
     * @param payload A Map containing "cancelledBy" (e.g., "MEMBER" or "ADMIN").
     * @return The updated booking with cancelled status or an error.
     */
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String bookingId, @RequestBody Map<String, String> payload) {
        String cancelledBy = payload.get("cancelledBy");
        if (cancelledBy == null || (!"MEMBER".equalsIgnoreCase(cancelledBy) && !"ADMIN".equalsIgnoreCase(cancelledBy))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Valid 'cancelledBy' (MEMBER or ADMIN) is required.");
        }
        try {
            Booking cancelledBooking = bookingService.cancelBooking(bookingId, cancelledBy.toUpperCase());
            return ResponseEntity.ok(cancelledBooking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error cancelling booking.");
        }
    }


    // Admin endpoint to view all bookings (optional)
    @GetMapping("/admin/all")
    public ResponseEntity<List<Booking>> getAllBookingsAdmin() {
        // Add security here in a real application to ensure only admins can access
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }
}

