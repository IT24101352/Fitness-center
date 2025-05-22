package com.PGN12.fitness_center_app.repository;

import com.PGN12.fitness_center_app.model.Feedback;
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
 * Repository for Feedback data, handles file-based storage in feedback.txt.
 */
@Repository
public class FeedbackRepository {

    private static final String FILE_NAME = "feedback.txt";
    private final Path filePath;
    private static final String DELIMITER = "|"; // Using pipe to avoid issues with commas

    public FeedbackRepository() {
        Path dataDir = Paths.get("src", "main", "resources", "data");
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                System.err.println("Error creating data directory for feedback: " + e.getMessage());
            }
        }
        this.filePath = dataDir.resolve(FILE_NAME);
        if (!Files.exists(this.filePath)) {
            try {
                Files.createFile(this.filePath);
                System.out.println("Feedback data file created: " + this.filePath.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error creating feedback data file: " + this.filePath.toAbsolutePath() + " - " + e.getMessage());
            }
        }
        System.out.println("FeedbackRepository initialized. Data file path: " + this.filePath.toAbsolutePath());
    }

    private String feedbackToString(Feedback feedback) {
        return String.join(DELIMITER,
                escape(feedback.getFeedbackId()),
                escape(feedback.getMemberId() == null ? "" : feedback.getMemberId()),
                escape(feedback.getReviewerName() == null ? "" : feedback.getReviewerName()),
                String.valueOf(feedback.isAnonymous()),
                escape(feedback.getFeedbackType()),
                escape(feedback.getTargetName() == null ? "" : feedback.getTargetName()),
                feedback.getRating() == null ? "" : String.valueOf(feedback.getRating()),
                escape(feedback.getComments()),
                escape(feedback.getSubmissionDate()),
                escape(feedback.getStatus()),
                escape(feedback.getAdminNotes() == null ? "" : feedback.getAdminNotes()),
                String.valueOf(feedback.isVerifiedReviewer())
        );
    }

    private Feedback stringToFeedback(String line) {
        String[] parts = line.split("\\" + DELIMITER, -1);
        if (parts.length >= 12) { // Number of fields in feedbackToString
            try {
                Feedback feedback = new Feedback();
                feedback.setFeedbackId(unescape(parts[0]));
                feedback.setMemberId(parts[1].isEmpty() ? null : unescape(parts[1]));
                feedback.setReviewerName(parts[2].isEmpty() ? null : unescape(parts[2]));
                feedback.setAnonymous(Boolean.parseBoolean(parts[3]));
                feedback.setFeedbackType(unescape(parts[4]));
                feedback.setTargetName(parts[5].isEmpty() ? null : unescape(parts[5]));
                feedback.setRating(parts[6].isEmpty() ? null : Integer.parseInt(parts[6]));
                feedback.setComments(unescape(parts[7]));
                feedback.setSubmissionDate(unescape(parts[8]));
                feedback.setStatus(unescape(parts[9]));
                feedback.setAdminNotes(parts[10].isEmpty() ? null : unescape(parts[10]));
                feedback.setVerifiedReviewer(Boolean.parseBoolean(parts[11]));
                return feedback;
            } catch (NumberFormatException e) {
                System.err.println("Error parsing rating in feedback.txt line: " + line + " - " + e.getMessage());
                return null;
            } catch (Exception e) {
                System.err.println("Error parsing feedback.txt line: " + line + " - " + e.getMessage());
                return null;
            }
        }
        System.err.println("Skipping malformed line in " + FILE_NAME + ": " + line + " (Expected 12 parts, got " + parts.length + ")");
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

    public List<Feedback> findAll() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.lines()
                    .map(this::stringToFeedback)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading from " + FILE_NAME + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<Feedback> findById(String feedbackId) {
        return findAll().stream()
                .filter(f -> f.getFeedbackId().equals(feedbackId))
                .findFirst();
    }

    public Feedback save(Feedback feedback) {
        List<Feedback> feedbacks = findAll();
        boolean updated = false;
        for (int i = 0; i < feedbacks.size(); i++) {
            if (feedbacks.get(i).getFeedbackId().equals(feedback.getFeedbackId())) {
                feedbacks.set(i, feedback); // Update existing
                updated = true;
                break;
            }
        }
        if (!updated) {
            feedbacks.add(feedback); // Add new
        }
        writeToFile(feedbacks);
        return feedback;
    }

    public boolean deleteById(String feedbackId) {
        List<Feedback> feedbacks = findAll();
        boolean removed = feedbacks.removeIf(f -> f.getFeedbackId().equals(feedbackId));
        if (removed) {
            writeToFile(feedbacks);
        }
        return removed;
    }

    private void writeToFile(List<Feedback> feedbacks) {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Feedback f : feedbacks) {
                writer.write(feedbackToString(f));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
