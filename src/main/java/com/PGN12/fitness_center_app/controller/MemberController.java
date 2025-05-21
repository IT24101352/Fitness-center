package com.PGN12.fitness_center_app.controller;

import com.PGN12.fitness_center_app.dto.LoginRequest;
import com.PGN12.fitness_center_app.dto.LoginResponse;
import com.PGN12.fitness_center_app.model.Member;
import com.PGN12.fitness_center_app.model.Payment; // For checking PENDING_PAYMENT invoice
import com.PGN12.fitness_center_app.service.MemberService;
import com.PGN12.fitness_center_app.service.PaymentService; // For checking PENDING_PAYMENT invoice
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.PGN12.fitness_center_app.dto.RegistrationResponse;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final PaymentService paymentService; // Inject for login check

    @Autowired
    public MemberController(MemberService memberService, PaymentService paymentService) {
        this.memberService = memberService;
        this.paymentService = paymentService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> createMember(@RequestBody Member member) {
        try {
            RegistrationResponse registrationResponse = memberService.createMemberAndInitialInvoice(member);
            return new ResponseEntity<>(registrationResponse, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during registration.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginMember(@RequestBody LoginRequest loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getPassword() == null ||
                loginRequest.getEmail().trim().isEmpty() || loginRequest.getPassword().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email and password are required.");
        }
        try {
            Optional<LoginResponse> loginResponseOpt = memberService.loginMember(loginRequest.getEmail(), loginRequest.getPassword());

            if (loginResponseOpt.isPresent()) {
                return ResponseEntity.ok(loginResponseOpt.get());
            } else {
                // Check if user exists but is PENDING_PAYMENT
                Optional<Member> memberOpt = memberService.getAllMembers().stream()
                        .filter(m -> m.getEmail().equalsIgnoreCase(loginRequest.getEmail()))
                        .findFirst();

                if (memberOpt.isPresent() && "PENDING_PAYMENT".equalsIgnoreCase(memberOpt.get().getStatus())) {
                    // Find the specific initial pending invoice for redirection guidance
                    Optional<Payment> pendingInitialInvoice = paymentService.getPaymentsByMemberId(memberOpt.get().getId())
                            .stream()
                            .filter(p -> "PENDING".equalsIgnoreCase(p.getStatus()) &&
                                    p.getDescription() != null &&
                                    p.getDescription().toLowerCase().contains("initial membership dues"))
                            .findFirst();
                    if (pendingInitialInvoice.isPresent()) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Account pending payment. Please complete your initial payment. Invoice ID: " + pendingInitialInvoice.get().getPaymentId());
                    }
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Account pending payment. Please complete your initial payment.");
                }
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password, or account not active.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during login.");
        }
    }

    @GetMapping
    public ResponseEntity<List<Member>> getAllMembers() {
        List<Member> members = memberService.getAllMembers();
        return ResponseEntity.ok(members);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberById(@PathVariable String id) {
        Optional<Member> member = memberService.getMemberById(id);
        return member.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMember(@PathVariable String id, @RequestBody Member memberDetails) {
        try {
            Optional<Member> updatedMemberOpt = memberService.updateMember(id, memberDetails);
            return updatedMemberOpt
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member with ID " + id + " not found."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while updating member.");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMember(@PathVariable String id) {
        boolean deleted = memberService.deleteMember(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member with ID " + id + " not found.");
        }
    }

    // New endpoint to get members sorted by last renewal date
    @GetMapping("/sorted-by-renewal")
    public ResponseEntity<List<Member>> getMembersSortedByRenewal(
            @RequestParam(required = false, defaultValue = "false") boolean ascending) {
        // Add Admin Role Check Here in a real app for this endpoint
        List<Member> sortedMembers = memberService.getMembersSortedByLastRenewalDate(ascending);
        return ResponseEntity.ok(sortedMembers);
    }
}