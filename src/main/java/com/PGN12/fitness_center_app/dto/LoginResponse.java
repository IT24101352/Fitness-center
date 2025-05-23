package com.PGN12.fitness_center_app.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for login responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token; // A simple session token
    private String memberId;
    private String fullName;
    private String email;
    private String role;
    private String message;
}
