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

/**
 * Repository for FitnessClass data, handles file-based storage in classes.txt.
 */
@Repository
public class FitnessClassRepository {

    private static final String FILE_NAME = "classes.txt";
    private final Path filePath;
    private static final String DELIMITER = "|"; // Using pipe to avoid issues with commas in description

    public FitnessClassRepository() {
        Path dataDir = Paths.get("src", "main", "resources", "data");
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                System.err.println("Error creating data directory for classes: " + e.getMessage());
            }
        }
        this.filePath = dataDir.resolve(FILE_NAME);
        if (!Files.exists(this.filePath)) {
            try {
                Files.createFile(this.filePath);
                System.out.println("Classes data file created: " + this.filePath.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error creating classes data file: " + this.filePath.toAbsolutePath() + " - " + e.getMessage());
            }
        }
        System.out.println("FitnessClassRepository initialized. Data file path: " + this.filePath.toAbsolutePath());
    }

    private String fitnessClassToString(FitnessClass fitnessClass) {
        return String.join(DELIMITER,
                escape(fitnessClass.getId()),
                escape(fitnessClass.getName()),
                escape(fitnessClass.getInstructor()),
                escape(fitnessClass.getDayOfWeek()),
                escape(fitnessClass.getTime()),
                escape(fitnessClass.getDuration()),
                String.valueOf(fitnessClass.getCapacity()),
                String.valueOf(fitnessClass.getBookedSpots()),
                escape(fitnessClass.getType()),
                escape(fitnessClass.getDescription()),
                escape(fitnessClass.getStatus())
        );
    }

    private FitnessClass stringToFitnessClass(String line) {
        String[] parts = line.split("\\" + DELIMITER, -1);
        if (parts.length >= 11) {
            try {
                FitnessClass fitnessClass = new FitnessClass();
                fitnessClass.setId(unescape(parts[0]));
                fitnessClass.setName(unescape(parts[1]));
                fitnessClass.setInstructor(unescape(parts[2]));
                fitnessClass.setDayOfWeek(unescape(parts[3]));
                fitnessClass.setTime(unescape(parts[4]));
                fitnessClass.setDuration(unescape(parts[5]));
                fitnessClass.setCapacity(Integer.parseInt(parts[6]));
                fitnessClass.setBookedSpots(Integer.parseInt(parts[7]));
                fitnessClass.setType(unescape(parts[8]));
                fitnessClass.setDescription(unescape(parts[9]));
                fitnessClass.setStatus(unescape(parts[10]));
                return fitnessClass;
            } catch (NumberFormatException e) {
                System.err.println("Error parsing number in classes.txt line: " + line + " - " + e.getMessage());
                return null;
            }
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

    public List<FitnessClass> findAll() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.lines()
                    .map(this::stringToFitnessClass)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading from " + FILE_NAME + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<FitnessClass> findById(String id) {
        return findAll().stream()
                .filter(fc -> fc.getId().equals(id))
                .findFirst();
    }

    public FitnessClass save(FitnessClass fitnessClass) {
        List<FitnessClass> classes = findAll();
        boolean updated = false;
        for (int i = 0; i < classes.size(); i++) {
            if (classes.get(i).getId().equals(fitnessClass.getId())) {
                classes.set(i, fitnessClass);
                updated = true;
                break;
            }
        }
        if (!updated) {
            classes.add(fitnessClass);
        }
        writeToFile(classes);
        return fitnessClass;
    }

    public boolean deleteById(String id) {
        List<FitnessClass> classes = findAll();
        boolean removed = classes.removeIf(fc -> fc.getId().equals(id));
        if (removed) {
            writeToFile(classes);
        }
        return removed;
    }

    private void writeToFile(List<FitnessClass> classes) {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (FitnessClass fc : classes) {
                writer.write(fitnessClassToString(fc));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
