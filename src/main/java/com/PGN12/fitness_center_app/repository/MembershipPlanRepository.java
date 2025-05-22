package com.PGN12.fitness_center_app.repository;

import com.PGN12.fitness_center_app.model.MembershipPlan;
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
 * Repository for MembershipPlan data, handles file-based storage in plans.txt.
 */
@Repository
public class MembershipPlanRepository {

    private static final String FILE_NAME = "plans.txt";
    private final Path filePath;
    private static final String DELIMITER = "|"; // Using a pipe delimiter for plans to avoid issues with commas in description
    private static final String FEATURE_DELIMITER = ";"; // For separating features within the description string

    public MembershipPlanRepository() {
        Path dataDir = Paths.get("src", "main", "resources", "data");
        // Ensure data directory exists (already handled by MemberRepository constructor if it runs first,
        // but good to have it here too for independence or if this repo is initialized first)
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                System.err.println("Error creating data directory for plans: " + e.getMessage());
            }
        }
        this.filePath = dataDir.resolve(FILE_NAME);
        if (!Files.exists(this.filePath)) {
            try {
                Files.createFile(this.filePath);
                System.out.println("Plans data file created: " + this.filePath.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error creating plans data file: " + this.filePath.toAbsolutePath() + " - " + e.getMessage());
            }
        }
        System.out.println("MembershipPlanRepository initialized. Data file path: " + this.filePath.toAbsolutePath());
    }

    /**
     * Converts a MembershipPlan object to a delimited string.
     * @param plan The plan object.
     * @return A string representation for file storage.
     */
    private String planToString(MembershipPlan plan) {
        return String.join(DELIMITER,
                escape(plan.getId()),
                escape(plan.getName()),
                String.valueOf(plan.getPrice()),
                escape(plan.getDuration()),
                escape(plan.getDescription()), // Store features as a single semicolon-delimited string
                escape(plan.getStatus())
        );
    }

    /**
     * Converts a delimited string from the file to a MembershipPlan object.
     * @param line The string from the file.
     * @return A MembershipPlan object, or null if malformed.
     */
    private MembershipPlan stringToPlan(String line) {
        String[] parts = line.split("\\" + DELIMITER, -1); // Need to escape pipe for regex
        if (parts.length >= 6) {
            try {
                MembershipPlan plan = new MembershipPlan();
                plan.setId(unescape(parts[0]));
                plan.setName(unescape(parts[1]));
                plan.setPrice(Double.parseDouble(parts[2]));
                plan.setDuration(unescape(parts[3]));
                plan.setDescription(unescape(parts[4])); // Read as a single string
                plan.setStatus(unescape(parts[5]));
                return plan;
            } catch (NumberFormatException e) {
                System.err.println("Error parsing price for plan line: " + line + " - " + e.getMessage());
                return null;
            }
        }
        System.err.println("Skipping malformed line in " + FILE_NAME + ": " + line);
        return null;
    }

    // Basic escape/unescape for the chosen delimiter.
    // For robust CSV/TSV, a library would be better.
    private String escape(String value) {
        if (value == null) return "";
        return value.replace(DELIMITER, "\\" + DELIMITER)
                .replace(FEATURE_DELIMITER, "\\" + FEATURE_DELIMITER)
                .replace("\n", "\\n");
    }

    private String unescape(String value) {
        if (value == null) return "";
        return value.replace("\\" + DELIMITER, DELIMITER)
                .replace("\\" + FEATURE_DELIMITER, FEATURE_DELIMITER)
                .replace("\\n", "\n");
    }


    public List<MembershipPlan> findAll() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.lines()
                    .map(this::stringToPlan)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading from " + FILE_NAME + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<MembershipPlan> findById(String id) {
        return findAll().stream()
                .filter(plan -> plan.getId().equals(id))
                .findFirst();
    }

    public MembershipPlan save(MembershipPlan plan) {
        List<MembershipPlan> plans = findAll();
        boolean updated = false;
        for (int i = 0; i < plans.size(); i++) {
            if (plans.get(i).getId().equals(plan.getId())) {
                plans.set(i, plan);
                updated = true;
                break;
            }
        }
        if (!updated) {
            plans.add(plan);
        }
        writeToFile(plans);
        return plan;
    }

    public boolean deleteById(String id) {
        List<MembershipPlan> plans = findAll();
        boolean removed = plans.removeIf(plan -> plan.getId().equals(id));
        if (removed) {
            writeToFile(plans);
        }
        return removed;
    }

    private void writeToFile(List<MembershipPlan> plans) {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (MembershipPlan plan : plans) {
                writer.write(planToString(plan));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
