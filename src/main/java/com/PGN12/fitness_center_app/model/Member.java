package com.PGN12.fitness_center_app.model;

import com.PGN12.fitness_center_app.service.PaymentService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    private String id;
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private String dateOfBirth; // Should be YYYY-MM-DD
    private String membershipTierId;
    private String status ; // e.g., "ACTIVE", "INACTIVE", "PENDING_PAYMENT"
    private String role; // e.g., "MEMBER", "ADMIN"
    private String joinDate; // Date of registration (YYYY-MM-DD)
    private String lastRenewalDate; // New field: Date of last renewal (YYYY-MM-DD)

}