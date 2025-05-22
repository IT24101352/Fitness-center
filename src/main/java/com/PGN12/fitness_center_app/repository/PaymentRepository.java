package com.PGN12.fitness_center_app.repository;


import com.PGN12.fitness_center_app.model.Payment;
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
public class PaymentRepository {

    private static final String FILE_NAME = "payments.txt";
    private final Path filePath;
    private static final String DELIMITER = "|"; // Using pipe to avoid issues with commas in description

    public PaymentRepository() {
        Path dataDir = Paths.get("src", "main", "resources", "data");
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                System.err.println("Error creating data directory for payments: " + e.getMessage());
            }
        }
        this.filePath = dataDir.resolve(FILE_NAME);
        if (!Files.exists(this.filePath)) {
            try {
                Files.createFile(this.filePath);
                System.out.println("Payments data file created: " + this.filePath.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error creating payments data file: " + this.filePath.toAbsolutePath() + " - " + e.getMessage());
            }
        }
        System.out.println("PaymentRepository initialized. Data file path: " + this.filePath.toAbsolutePath());
    }

    private String paymentToString(Payment payment) {
        return String.join(DELIMITER,
                escape(payment.getPaymentId()),
                escape(payment.getMemberId()),
                escape(payment.getMemberName()),
                escape(payment.getPlanId()),
                escape(payment.getPlanName()),
                escape(payment.getDescription()),
                String.valueOf(payment.getAmount()),
                escape(payment.getCurrency()),
                escape(payment.getIssueDate()),
                escape(payment.getDueDate() == null ? "" : payment.getDueDate()),
                escape(payment.getPaymentDate() == null ? "" : payment.getPaymentDate()),
                escape(payment.getStatus()),
                escape(payment.getPaymentMethod() == null ? "" : payment.getPaymentMethod()),
                escape(payment.getTransactionNotes() == null ? "" : payment.getTransactionNotes())
        );
    }

    private Payment stringToPayment(String line) {
        String[] parts = line.split("\\" + DELIMITER, -1); // -1 to keep trailing empty strings
        if (parts.length >= 14) { // Number of fields in paymentToString
            try {
                Payment payment = new Payment();
                payment.setPaymentId(unescape(parts[0]));
                payment.setMemberId(unescape(parts[1]));
                payment.setMemberName(unescape(parts[2]));
                payment.setPlanId(unescape(parts[3]));
                payment.setPlanName(unescape(parts[4]));
                payment.setDescription(unescape(parts[5]));
                payment.setAmount(Double.parseDouble(parts[6]));
                payment.setCurrency(unescape(parts[7]));
                payment.setIssueDate(unescape(parts[8]));
                payment.setDueDate(parts[9].isEmpty() ? null : unescape(parts[9]));
                payment.setPaymentDate(parts[10].isEmpty() ? null : unescape(parts[10]));
                payment.setStatus(unescape(parts[11]));
                payment.setPaymentMethod(parts[12].isEmpty() ? null : unescape(parts[12]));
                payment.setTransactionNotes(parts[13].isEmpty() ? null : unescape(parts[13]));
                return payment;
            } catch (NumberFormatException e) {
                System.err.println("Error parsing amount in payments.txt line: " + line + " - " + e.getMessage());
                return null;
            }
        }
        System.err.println("Skipping malformed line in " + FILE_NAME + ": " + line + " (Expected 14 parts, got " + parts.length + ")");
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

    public List<Payment> findAll() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.lines()
                    .map(this::stringToPayment)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading from " + FILE_NAME + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<Payment> findById(String paymentId) {
        return findAll().stream()
                .filter(p -> p.getPaymentId().equals(paymentId))
                .findFirst();
    }

    public List<Payment> findByMemberId(String memberId) {
        return findAll().stream()
                .filter(p -> p.getMemberId().equals(memberId))
                .collect(Collectors.toList());
    }

    public Payment save(Payment payment) {
        List<Payment> payments = findAll();
        boolean updated = false;
        for (int i = 0; i < payments.size(); i++) {
            if (payments.get(i).getPaymentId().equals(payment.getPaymentId())) {
                payments.set(i, payment); // Update existing
                updated = true;
                break;
            }
        }
        if (!updated) {
            payments.add(payment); // Add new
        }
        writeToFile(payments);
        return payment;
    }

    public boolean deleteById(String paymentId) {
        List<Payment> payments = findAll();
        boolean removed = payments.removeIf(p -> p.getPaymentId().equals(paymentId));
        if (removed) {
            writeToFile(payments);
        }
        return removed;
    }

    private void writeToFile(List<Payment> payments) {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Payment p : payments) {
                writer.write(paymentToString(p));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
