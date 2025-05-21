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
 * Service layer for FitnessClass business logic.
 */
@Service
public class FitnessClassService {

    private final FitnessClassRepository fitnessClassRepository;

    @Autowired
    public FitnessClassService(FitnessClassRepository fitnessClassRepository) {
        this.fitnessClassRepository = fitnessClassRepository;
    }

    public List<FitnessClass> getAllClasses() {
        return fitnessClassRepository.findAll();
    }

    /**
     * Gets all active classes.
     * @return List of active FitnessClass objects.
     */
    public List<FitnessClass> getAllActiveClasses() {
        return fitnessClassRepository.findAll().stream()
                .filter(fc -> "active".equalsIgnoreCase(fc.getStatus()))
                .collect(Collectors.toList());
    }

    public Optional<FitnessClass> getClassById(String id) {
        return fitnessClassRepository.findById(id);
    }

    public FitnessClass createClass(FitnessClass fitnessClass) {
        if (fitnessClass.getName() == null || fitnessClass.getName().trim().isEmpty() ||
                fitnessClass.getInstructor() == null || fitnessClass.getInstructor().trim().isEmpty() ||
                fitnessClass.getDayOfWeek() == null || fitnessClass.getTime() == null ||
                fitnessClass.getDuration() == null || fitnessClass.getCapacity() <= 0) {
            throw new IllegalArgumentException("Class name, instructor, day, time, duration, and capacity are required and capacity must be positive.");
        }

        if (fitnessClass.getId() == null || fitnessClass.getId().isEmpty()) {
            fitnessClass.setId("CLS" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        if (fitnessClass.getStatus() == null || fitnessClass.getStatus().isEmpty()) {
            fitnessClass.setStatus("active"); // Default to active
        }
        fitnessClass.setBookedSpots(0); // New classes start with 0 booked spots

        return fitnessClassRepository.save(fitnessClass);
    }

    public Optional<FitnessClass> updateClass(String id, FitnessClass classDetails) {
        Optional<FitnessClass> existingClassOpt = fitnessClassRepository.findById(id);
        if (existingClassOpt.isPresent()) {
            FitnessClass existingClass = existingClassOpt.get();
            // Update fields if provided
            if (classDetails.getName() != null) existingClass.setName(classDetails.getName());
            if (classDetails.getInstructor() != null) existingClass.setInstructor(classDetails.getInstructor());
            if (classDetails.getDayOfWeek() != null) existingClass.setDayOfWeek(classDetails.getDayOfWeek());
            if (classDetails.getTime() != null) existingClass.setTime(classDetails.getTime());
            if (classDetails.getDuration() != null) existingClass.setDuration(classDetails.getDuration());
            if (classDetails.getCapacity() > 0) existingClass.setCapacity(classDetails.getCapacity());
            // bookedSpots should ideally be managed by booking service, not directly updated here unless for admin override
            if (classDetails.getType() != null) existingClass.setType(classDetails.getType());
            if (classDetails.getDescription() != null) existingClass.setDescription(classDetails.getDescription());
            if (classDetails.getStatus() != null) existingClass.setStatus(classDetails.getStatus());

            return Optional.of(fitnessClassRepository.save(existingClass));
        }
        return Optional.empty();
    }

    public boolean deleteClass(String id) {
        return fitnessClassRepository.deleteById(id);
    }

    /**
     * Attempts to increment the booked spots for a class.
     * This method should be called when a booking is made.
     * @param classId The ID of the class.
     * @return true if spots were incremented, false if class is full or not found.
     */
    public boolean incrementBookedSpotForClass(String classId) {
        Optional<FitnessClass> classOpt = fitnessClassRepository.findById(classId);
        if (classOpt.isPresent()) {
            FitnessClass fitnessClass = classOpt.get();
            if (fitnessClass.incrementBookedSpots()) {
                fitnessClassRepository.save(fitnessClass);
                return true;
            }
        }
        return false;
    }

    /**
     * Decrements the booked spots for a class.
     * This method should be called when a booking is cancelled.
     * @param classId The ID of the class.
     */
    public void decrementBookedSpotForClass(String classId) {
        Optional<FitnessClass> classOpt = fitnessClassRepository.findById(classId);
        if (classOpt.isPresent()) {
            FitnessClass fitnessClass = classOpt.get();
            fitnessClass.decrementBookedSpots();
            fitnessClassRepository.save(fitnessClass);
        }
    }
}
