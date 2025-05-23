package com.PGN12.fitness_center_app.service;

import com.PGN12.fitness_center_app.model.Booking;
import com.PGN12.fitness_center_app.model.FitnessClass;
import com.PGN12.fitness_center_app.model.Member;
import com.PGN12.fitness_center_app.repository.BookingRepository;
import com.PGN12.fitness_center_app.repository.FitnessClassRepository;
import com.PGN12.fitness_center_app.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for handling booking operations and business logic.
 * Manages the complete lifecycle of class bookings including creation,
 * cancellation, and status management.
 */
@Service
public class BookingService {

    // Dependency repositories and services
    private final BookingRepository bookingRepository;
    private final FitnessClassRepository fitnessClassRepository;
    private final MemberRepository memberRepository;
    private final FitnessClassService fitnessClassService;

    /**
     * Constructor with dependency injection
     * @param bookingRepository Repository for booking data access
     * @param fitnessClassRepository Repository for class information
     * @param memberRepository Repository for member information
     * @param fitnessClassService Service for class spot management
     */
    @Autowired
    public BookingService(BookingRepository bookingRepository,
                          FitnessClassRepository fitnessClassRepository,
                          MemberRepository memberRepository,
                          FitnessClassService fitnessClassService) {
        this.bookingRepository = bookingRepository;
        this.fitnessClassRepository = fitnessClassRepository;
        this.memberRepository = memberRepository;
        this.fitnessClassService = fitnessClassService;
    }

    /**
     * Retrieves all bookings in the system
     * @return List of all bookings
     */
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /**
     * Finds a specific booking by its ID
     * @param bookingId The ID of the booking to find
     * @return Optional containing the booking if found
     */
    public Optional<Booking> getBookingById(String bookingId) {
        return bookingRepository.findById(bookingId);
    }

    /**
     * Retrieves all bookings for a specific member
     * @param memberId The ID of the member
     * @return List of the member's bookings
     */
    public List<Booking> getBookingsByMemberId(String memberId) {
        return bookingRepository.findByMemberId(memberId);
    }

    /**
     * Creates a new booking after validating all constraints
     * @param memberId ID of the member making the booking
     * @param classId ID of the class to book
     * @return The created booking
     * @throws IllegalArgumentException if validation fails
     */
    public Booking createBooking(String memberId, String classId) {
        // Validate member exists
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("Member with ID " + memberId + " not found.");
        }

        // Validate class exists and is active
        Optional<FitnessClass> classOpt = fitnessClassRepository.findById(classId);
        if (classOpt.isEmpty()) {
            throw new IllegalArgumentException("Class with ID " + classId + " not found.");
        }

        FitnessClass fitnessClass = classOpt.get();
        if (!"active".equalsIgnoreCase(fitnessClass.getStatus())) {
            throw new IllegalArgumentException("Class '" + fitnessClass.getName() + "' is not active and cannot be booked.");
        }

        // Check class capacity
        if (!fitnessClass.hasAvailableSpots()) {
            throw new IllegalArgumentException("Class '" + fitnessClass.getName() + "' is full.");
        }

        // Prevent duplicate bookings
        boolean alreadyBooked = bookingRepository.findByMemberId(memberId).stream()
                .anyMatch(b -> b.getClassId().equals(classId) &&
                        "CONFIRMED".equalsIgnoreCase(b.getStatus()));
        if (alreadyBooked) {
            throw new IllegalArgumentException("Member " + memberId +
                    " is already booked for class " + classId + ".");
        }

        // Reserve spot in class
        boolean spotIncremented = fitnessClassService.incrementBookedSpotForClass(classId);
        if (!spotIncremented) {
            throw new RuntimeException("Failed to reserve spot for class " + classId +
                    ". Class might have become full concurrently.");
        }

        // Create and save booking
        Booking booking = new Booking();
        booking.setBookingId("BOOK" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        booking.setMemberId(memberId);
        booking.setClassId(classId);
        booking.setBookingTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        booking.setStatus("CONFIRMED");

        return bookingRepository.save(booking);
    }

    /**
     * Cancels an existing booking
     * @param bookingId ID of the booking to cancel
     * @param cancelledBy Who is cancelling ("MEMBER" or "ADMIN")
     * @return The updated booking
     * @throws IllegalArgumentException if validation fails
     */
    public Booking cancelBooking(String bookingId, String cancelledBy) {
        // Validate booking exists and is cancellable
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new IllegalArgumentException("Booking with ID " + bookingId + " not found.");
        }

        Booking booking = bookingOpt.get();
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Booking " + bookingId +
                    " is not in a state that can be cancelled.");
        }

        // Free up class spot
        fitnessClassService.decrementBookedSpotForClass(booking.getClassId());

        // Set appropriate cancellation status
        if ("MEMBER".equalsIgnoreCase(cancelledBy)) {
            booking.setStatus("CANCELLED_MEMBER");
        } else if ("ADMIN".equalsIgnoreCase(cancelledBy)) {
            booking.setStatus("CANCELLED_ADMIN");
        } else {
            throw new IllegalArgumentException(
                    "Invalid 'cancelledBy' value. Must be MEMBER or ADMIN.");
        }

        // Update timestamp and save
        booking.setBookingTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return bookingRepository.save(booking);
    }
}