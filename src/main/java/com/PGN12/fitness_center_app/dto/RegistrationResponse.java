package com.PGN12.fitness_center_app.dto;

import com.PGN12.fitness_center_app.model.Member;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {
    private String message;
    private String memberId;
    private String invoiceId; // The ID of the initial invoice to be paid
    private Member createdMemberData; // Optional: send back the created member data
}
