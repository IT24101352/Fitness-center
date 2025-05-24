package com.PGN12.fitness_center_app.repository;

import com.PGN12.fitness_center_app.model.Member;
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
 * Repository for Member data, handles file-based storage in members.txt.
 */
@Repository
public class MemberRepository {

    private static final String FILE_NAME = "members.txt";
    private final Path filePath;
    private static final String DELIMITER = ","; // Ensure this is consistent

    public MemberRepository() {
        Path dataDir = Paths.get("src", "main", "resources", "data");
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
                System.out.println("Data directory created: " + dataDir.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error creating data directory: " + dataDir.toAbsolutePath() + " - " + e.getMessage());
            }
        }
        this.filePath = dataDir.resolve(FILE_NAME);
        if (!Files.exists(this.filePath)) {
            try {
                Files.createFile(this.filePath);
                System.out.println("Data file created: " + this.filePath.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error creating data file: " + this.filePath.toAbsolutePath() + " - " + e.getMessage());
            }
        }
        System.out.println("MemberRepository initialized. Data file path: " + this.filePath.toAbsolutePath());
    }

    private String memberToString(Member member) {
        return String.join(DELIMITER,
                escapeCsv(member.getId()),
                escapeCsv(member.getFullName()),
                escapeCsv(member.getEmail()),
                escapeCsv(member.getPassword()),
                escapeCsv(member.getPhone()),
                escapeCsv(member.getDateOfBirth()),
                escapeCsv(member.getMembershipTierId()),
                escapeCsv(member.getRole() == null ? "MEMBER" : member.getRole()),
                escapeCsv(member.getStatus() == null ? "PENDING_PAYMENT" : member.getStatus()),
                escapeCsv(member.getJoinDate() == null ? "" : member.getJoinDate()),
                escapeCsv(member.getLastRenewalDate() == null ? "" : member.getLastRenewalDate()) // New field
        );
    }

    private Member stringToMember(String line) {
        String[] parts = line.split(DELIMITER, -1); // -1 to keep trailing empty strings
        // Expecting 11 parts now with lastRenewalDate
        if (parts.length >= 11) {
            Member member = new Member();
            member.setId(unescapeCsv(parts[0]));
            member.setFullName(unescapeCsv(parts[1]));
            member.setEmail(unescapeCsv(parts[2]));
            member.setPassword(unescapeCsv(parts[3]));
            member.setPhone(unescapeCsv(parts[4]));
            member.setDateOfBirth(unescapeCsv(parts[5]));
            member.setMembershipTierId(unescapeCsv(parts[6]));
            member.setRole(unescapeCsv(parts[7]));
            member.setStatus(unescapeCsv(parts[8]));
            member.setJoinDate(unescapeCsv(parts[9]));
            member.setLastRenewalDate(unescapeCsv(parts[10])); // New field
            return member;
        }
        System.err.println("Skipping malformed line in " + FILE_NAME + ": " + line + " (Expected 11 parts, got " + parts.length + ")");
        return null;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(DELIMITER) || escaped.contains("\"") || escaped.contains("\n")) {
            escaped = "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String unescapeCsv(String value) {
        if (value == null) return "";
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace("\"\"", "\"");
    }

    public List<Member> findAll() { // read all members
        if (!Files.exists(filePath)) {
            System.out.println(FILE_NAME + " not found, returning empty list.");
            return new ArrayList<>();
        }
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.lines()
                    .map(this::stringToMember)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading from " + FILE_NAME + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<Member> findById(String id) {  // read member from its id
        return findAll().stream()
                .filter(member -> member.getId().equals(id))
                .findFirst();
    }

    public Optional<Member> findByEmail(String email) {
        return findAll().stream()
                .filter(member -> member.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public Member save(Member member) { //create a list and store member objects
        List<Member> members = findAll();
        boolean updated = false;
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getId().equals(member.getId())) {
                members.set(i, member);
                updated = true;
                break;
            }
        }
        if (!updated) {
            if (member.getRole() == null || member.getRole().trim().isEmpty()) {
                member.setRole("MEMBER");
            }
            members.add(member);
        }
        writeToFile(members);
        return member;
    }

    public boolean deleteById(String id) { //deleting a member by entering ID
        List<Member> members = findAll();
        boolean removed = members.removeIf(member -> member.getId().equals(id));
        if (removed) {
            writeToFile(members);
        }
        return removed;
    }

    private void writeToFile(List<Member> members) {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Member member : members) {
                writer.write(memberToString(member));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to " + FILE_NAME + ": " + e.getMessage());
        }
    }
}