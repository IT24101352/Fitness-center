package com.PGN12.fitness_center_app.service;

import com.PGN12.fitness_center_app.model.FitnessClass;
import com.PGN12.fitness_center_app.repository.FitnessClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for FitnessClass business logic and operations.
 * Handles all business rules and validations related to fitness classes.
 */
@Service
public class FitnessClassService {

    private final FitnessClassRepository fitnessClassRepository;

    /**
     * Constructor with dependency injection
     * @param fitnessClassRepository The repository for fitness class data access
     */
    @Autowired
    public FitnessClassService(FitnessClassRepository fitnessClassRepository) {
        this.fitnessClassRepository = fitnessClassRepository;
    }

    /**
     * Retrieves all fitness classes
     * @return List of all FitnessClass objects
     */
    public List<FitnessClass> getAllClasses() {
        return fitnessClassRepository.findAll();
    }

    /**
     * Retrieves only active fitness classes
     * @return List of active FitnessClass objects
     */
    public List<FitnessClass> getAllActiveClasses() {
        return fitnessClassRepository.findAll().stream()
                .filter(fc -> "active".equalsIgnoreCase(fc.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Finds a fitness class by its ID
     * @param id The ID of the class to find
     * @return Optional containing the found class or empty if not found
     */
    public Optional<FitnessClass> getClassById(String id) {
        return fitnessClassRepository.findById(id);
    }

    /**
     * Creates a new fitness class with validation
     * @param fitnessClass The class to create
     * @return The created fitness class
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public FitnessClass createClass(FitnessClass fitnessClass) {
        // Validate required fields
        if (fitnessClass.getName() == null || fitnessClass.getName().trim().isEmpty() ||
                fitnessClass.getInstructor() == null || fitnessClass.getInstructor().trim().isEmpty() ||
                fitnessClass.getDayOfWeek() == null || fitnessClass.getTime() == null ||
                fitnessClass.getDuration() == null || fitnessClass.getCapacity() <= 0) {
            throw new IllegalArgumentException(
                    "Class name, instructor, day, time, duration, and capacity are required " +
                            "and capacity must be positive.");
        }

        // Set default values if not provided
        if (fitnessClass.getId() == null || fitnessClass.getId().isEmpty()) {
            fitnessClass.setId("CLS" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        if (fitnessClass.getStatus() == null || fitnessClass.getStatus().isEmpty()) {
            fitnessClass.setStatus("active"); // Default status
        }
        fitnessClass.setBookedSpots(0); // Initialize with 0 bookings

        return fitnessClassRepository.save(fitnessClass);
    }

    /**
     * Updates an existing fitness class
     * @param id The ID of the class to update
     * @param classDetails The new class details
     * @return Optional containing the updated class or empty if not found
     */
    public Optional<FitnessClass> updateClass(String id, FitnessClass classDetails) {
        Optional<FitnessClass> existingClassOpt = fitnessClassRepository.findById(id);

        if (existingClassOpt.isPresent()) {
            FitnessClass existingClass = existingClassOpt.get();

            // Update only non-null fields (partial update)
            if (classDetails.getName() != null) {
                existingClass.setName(classDetails.getName());
            }
            if (classDetails.getInstructor() != null) {
                existingClass.setInstructor(classDetails.getInstructor());
            }
            if (classDetails.getDayOfWeek() != null) {
                existingClass.setDayOfWeek(classDetails.getDayOfWeek());
            }
            if (classDetails.getTime() != null) {
                existingClass.setTime(classDetails.getTime());
            }
            if (classDetails.getDuration() != null) {
                existingClass.setDuration(classDetails.getDuration());
            }
            if (classDetails.getCapacity() > 0) {
                existingClass.setCapacity(classDetails.getCapacity());
            }
            if (classDetails.getType() != null) {
                existingClass.setType(classDetails.getType());
            }
            if (classDetails.getDescription() != null) {
                existingClass.setDescription(classDetails.getDescription());
            }
            if (classDetails.getStatus() != null) {
                existingClass.setStatus(classDetails.getStatus());
            }

            return Optional.of(fitnessClassRepository.save(existingClass));
        }
        return Optional.empty();
    }

    /**
     * Deletes a fitness class
     * @param id The ID of the class to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteClass(String id) {
        return fitnessClassRepository.deleteById(id);
    }

    /**
     * Increments the booked spots for a class when a booking is made
     * @param classId The ID of the class to update
     * @return true if spots were incremented, false if class is full or not found
     */
    public boolean incrementBookedSpotForClass(String classId) {
        Optional<FitnessClass> classOpt = fitnessClassRepository.findById(classId);

        if (classOpt.isPresent()) {
            FitnessClass fitnessClass = classOpt.get();
            if (fitnessClass.incrementBookedSpots()) { // Checks capacity first
                fitnessClassRepository.save(fitnessClass);
                return true;
            }
        }
        return false;
    }

    /**
     * Decrements the booked spots for a class when a booking is cancelled
     * @param classId The ID of the class to update
     */
    public void decrementBookedSpotForClass(String classId) {
        Optional<FitnessClass> classOpt = fitnessClassRepository.findById(classId);

        if (classOpt.isPresent()) {
            FitnessClass fitnessClass = classOpt.get();
            fitnessClass.decrementBookedSpots(); // Decreases count (with bounds check)
            fitnessClassRepository.save(fitnessClass);
        }
    }
}