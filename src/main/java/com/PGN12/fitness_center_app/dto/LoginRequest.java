package com.PGN12.fitness_center_app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for login requests.
 */
@Data
@NoArgsConstructor
public class LoginRequest {
    private String email;
    private String password;
}
