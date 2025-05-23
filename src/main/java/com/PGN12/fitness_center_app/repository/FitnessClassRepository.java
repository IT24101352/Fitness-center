package com.PGN12.fitness_center_app.repository;

import com.PGN12.fitness_center_app.model.FitnessClass;
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

@Repository
public class FitnessClassRepository {
    private static final String FILE_NAME = "classes.txt";
    private final Path filePath;
    private static final String DELIMITER = "|";

    public FitnessClassRepository() {
        // Initialize the data directory and file
        Path dataDir = Paths.get("src", "main", "resources", "data");
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            this.filePath = dataDir.resolve(FILE_NAME);
            if (!Files.exists(this.filePath)) {
                Files.createFile(this.filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize repository storage", e);
        }
    }

    // =============== CRUD OPERATIONS =============== //

    /**
     * CREATE/UPDATE - Saves a fitness class to the repository
     * If the class already exists (same ID), it will be updated
     * If the class doesn't exist, it will be added
     *
     * @param fitnessClass The class to save
     * @return The saved fitness class
     */
    public FitnessClass save(FitnessClass fitnessClass) {
        // 1. Read all existing classes
        List<FitnessClass> classes = findAll();

        // 2. Check if class exists
        boolean exists = false;
        for (int i = 0; i < classes.size(); i++) {
            if (classes.get(i).getId().equals(fitnessClass.getId())) {
                // 3a. Update existing record
                classes.set(i, fitnessClass);
                exists = true;
                break;
            }
        }

        // 3b. Add new record if didn't exist
        if (!exists) {
            classes.add(fitnessClass);
        }

        // 4. Write all classes back to file
        writeToFile(classes);
        return fitnessClass;
    }

    /**
     * READ - Retrieves all fitness classes from the repository
     *
     * @return List of all fitness classes
     */
    public List<FitnessClass> findAll() {
        // 1. Return empty list if file doesn't exist
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            // 2. Read all lines from file
            return reader.lines()
                    // 3. Convert each line to FitnessClass object
                    .map(this::stringToFitnessClass)
                    // 4. Filter out any null values from malformed lines
                    .filter(fc -> fc != null)
                    // 5. Collect results into List
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read fitness classes", e);
        }
    }

    /**
     * READ - Finds a fitness class by its ID
     *
     * @param id The ID to search for
     * @return Optional containing the found class or empty if not found
     */
    public Optional<FitnessClass> findById(String id) {
        // 1. Get all classes
        return findAll().stream()
                // 2. Filter by matching ID
                .filter(fc -> fc.getId().equals(id))
                // 3. Return first match (if any)
                .findFirst();
    }

    /**
     * DELETE - Removes a fitness class by its ID
     *
     * @param id The ID of the class to remove
     * @return true if class was found and deleted, false otherwise
     */
    public boolean deleteById(String id) {
        // 1. Get all classes
        List<FitnessClass> classes = findAll();

        // 2. Remove class with matching ID
        boolean removed = classes.removeIf(fc -> fc.getId().equals(id));

        if (removed) {
            // 3. If class was removed, update the file
            writeToFile(classes);
        }

        return removed;
    }

    // =============== HELPER METHODS =============== //

    /**
     * Writes all fitness classes to the data file
     * Overwrites the entire file each time
     *
     * @param classes List of fitness classes to write
     */
    private void writeToFile(List<FitnessClass> classes) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            // Write each class as a line in the file
            for (FitnessClass fc : classes) {
                writer.write(fitnessClassToString(fc));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save fitness classes", e);
        }
    }

    /**
     * Converts a FitnessClass object to a delimited string
     */
    private String fitnessClassToString(FitnessClass fc) {
        return String.join(DELIMITER,
                escape(fc.getId()),
                escape(fc.getName()),
                escape(fc.getInstructor()),
                escape(fc.getDayOfWeek()),
                escape(fc.getTime()),
                escape(fc.getDuration()),
                String.valueOf(fc.getCapacity()),
                String.valueOf(fc.getBookedSpots()),
                escape(fc.getType()),
                escape(fc.getDescription()),
                escape(fc.getStatus())
        );
    }

    /**
     * Parses a delimited string into a FitnessClass object
     */
    private FitnessClass stringToFitnessClass(String line) {
        String[] parts = line.split("\\" + DELIMITER, -1);
        if (parts.length == 11) {
            FitnessClass fc = new FitnessClass();
            fc.setId(unescape(parts[0]));
            fc.setName(unescape(parts[1]));
            fc.setInstructor(unescape(parts[2]));
            fc.setDayOfWeek(unescape(parts[3]));
            fc.setTime(unescape(parts[4]));
            fc.setDuration(unescape(parts[5]));
            fc.setCapacity(Integer.parseInt(parts[6]));
            fc.setBookedSpots(Integer.parseInt(parts[7]));
            fc.setType(unescape(parts[8]));
            fc.setDescription(unescape(parts[9]));
            fc.setStatus(unescape(parts[10]));
            return fc;
        }
        return null;
    }

    /**
     * Escapes special characters for storage
     */
    private String escape(String value) {
        if (value == null) return "";
        return value.replace(DELIMITER, "\\" + DELIMITER)
                .replace("\n", "\\n");
    }

    /**
     * Unescapes special characters after reading
     */
    private String unescape(String value) {
        if (value == null) return "";
        return value.replace("\\n", "\n")
                .replace("\\" + DELIMITER, DELIMITER);
    }
}