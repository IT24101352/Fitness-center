package com.PGN12.fitness_center_app.service;

import com.PGN12.fitness_center_app.model.Booking;
import com.PGN12.fitness_center_app.model.FitnessClass;
import com.PGN12.fitness_center_app.model.Member;
import com.PGN12.fitness_center_app.repository.BookingRepository;
import com.PGN12.fitness_center_app.repository.FitnessClassRepository; // To update booked spots
import com.PGN12.fitness_center_app.repository.MemberRepository; // To check if member exists
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FitnessClassRepository fitnessClassRepository; // Renamed for clarity
    private final MemberRepository memberRepository;
    private final FitnessClassService fitnessClassService; // To use its methods for spot management

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

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(String bookingId) {
        return bookingRepository.findById(bookingId);
    }

    public List<Booking> getBookingsByMemberId(String memberId) {
        // You might want to enrich this with class details before returning
        return bookingRepository.findByMemberId(memberId);
    }

    /**
     * Creates a new booking for a member for a specific class.
     *
     * @param memberId The ID of the member making the booking.
     * @param classId  The ID of the class to be booked.
     * @return The created Booking object.
     * @throws IllegalArgumentException if member or class not found, class is full, or member already booked.
     */
    public Booking createBooking(String memberId, String classId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("Member with ID " + memberId + " not found.");
        }

        Optional<FitnessClass> classOpt = fitnessClassRepository.findById(classId);
        if (classOpt.isEmpty()) {
            throw new IllegalArgumentException("Class with ID " + classId + " not found.");
        }

        FitnessClass fitnessClass = classOpt.get();
        if (!"active".equalsIgnoreCase(fitnessClass.getStatus())) {
            throw new IllegalArgumentException("Class '" + fitnessClass.getName() + "' is not active and cannot be booked.");
        }

        if (!fitnessClass.hasAvailableSpots()) {
            throw new IllegalArgumentException("Class '" + fitnessClass.getName() + "' is full.");
        }

        // Check if member is already booked for this class
        boolean alreadyBooked = bookingRepository.findByMemberId(memberId).stream()
                .anyMatch(b -> b.getClassId().equals(classId) && "CONFIRMED".equalsIgnoreCase(b.getStatus()));
        if (alreadyBooked) {
            throw new IllegalArgumentException("Member " + memberId + " is already booked for class " + classId + ".");
        }

        // Increment booked spot in FitnessClass
        boolean spotIncremented = fitnessClassService.incrementBookedSpotForClass(classId);
        if (!spotIncremented) {
            // This should ideally not happen if hasAvailableSpots() check passed, but as a safeguard:
            throw new RuntimeException("Failed to increment booked spot for class " + classId + ". Class might have become full concurrently.");
        }

        Booking booking = new Booking();
        booking.setBookingId("BOOK" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        booking.setMemberId(memberId);
        booking.setClassId(classId);
        booking.setBookingTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        booking.setStatus("CONFIRMED");

        return bookingRepository.save(booking);
    }

    /**
     * Cancels a booking.
     *
     * @param bookingId The ID of the booking to cancel.
     * @param cancelledBy ("MEMBER" or "ADMIN")
     * @return The updated Booking object with status "CANCELLED_MEMBER" or "CANCELLED_ADMIN".
     * @throws IllegalArgumentException if booking not found.
     */
    public Booking cancelBooking(String bookingId, String cancelledBy) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new IllegalArgumentException("Booking with ID " + bookingId + " not found.");
        }

        Booking booking = bookingOpt.get();
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Booking " + bookingId + " is not in a state that can be cancelled.");
        }

        // Decrement booked spot in FitnessClass
        fitnessClassService.decrementBookedSpotForClass(booking.getClassId());

        if ("MEMBER".equalsIgnoreCase(cancelledBy)) {
            booking.setStatus("CANCELLED_MEMBER");
        } else if ("ADMIN".equalsIgnoreCase(cancelledBy)) {
            booking.setStatus("CANCELLED_ADMIN");
        } else {
            throw new IllegalArgumentException("Invalid 'cancelledBy' value. Must be MEMBER or ADMIN.");
        }
        booking.setBookingTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); // Update timestamp to cancellation time

        return bookingRepository.save(booking);
    }
}
