package com.PGN12.fitness_center_app.controller;

import com.PGN12.fitness_center_app.model.Payment;
import com.PGN12.fitness_center_app.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.format.annotation.DateTimeFormat; // Not used in current generate-invoice
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/generate-invoice")
    public ResponseEntity<?> generateInvoice(@RequestBody Map<String, String> payload) {
        try {
            String memberId = payload.get("memberId");
            String planId = payload.get("planId"); // Can be null for manual non-plan invoices
            String description = payload.get("description");
            double amount = Double.parseDouble(payload.getOrDefault("amount", "0"));
            String currency = payload.getOrDefault("currency", "USD");
            LocalDate dueDate = payload.containsKey("dueDate") ? LocalDate.parse(payload.get("dueDate"), DateTimeFormatter.ISO_LOCAL_DATE) : null;

            // If planId is not provided, ensure amount and description are.
            if ( (planId == null || planId.trim().isEmpty()) && (amount <= 0 || description == null || description.trim().isEmpty()) ) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("For non-plan based invoices, amount and description are required.");
            }
            if (memberId == null ) { // Plan ID is optional for manual invoices, but member ID is not
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("memberId is required.");
            }


            Payment invoice = paymentService.generateInvoice(memberId, planId, description, amount, currency, dueDate);
            return new ResponseEntity<>(invoice, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating invoice: " + e.getMessage());
        }
    }

    @PostMapping("/{paymentId}/pay")
    public ResponseEntity<?> processPayment(@PathVariable String paymentId, @RequestBody Map<String, String> payload) {
        try {
            String paymentMethod = payload.get("paymentMethod");
            double paymentAmount = Double.parseDouble(payload.get("paymentAmount"));

            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("paymentMethod is required.");
            }
            if (paymentAmount <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("paymentAmount must be positive.");
            }

            Payment paidInvoice = paymentService.processPayment(paymentId, paymentMethod, paymentAmount);
            return ResponseEntity.ok(paidInvoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing payment: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments(
            @RequestParam(required = false) String memberId,
            @RequestParam(required = false) String status) {
        List<Payment> payments = paymentService.getAllPayments();
        if (memberId != null && !memberId.isEmpty()) {
            payments = payments.stream().filter(p -> p.getMemberId().equals(memberId)).collect(java.util.stream.Collectors.toList());
        }
        if (status != null && !status.isEmpty()) {
            payments = payments.stream().filter(p -> p.getStatus().equalsIgnoreCase(status)).collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<Payment>> getPaymentsByMemberId(@PathVariable String memberId) {
        List<Payment> payments = paymentService.getPaymentsByMemberId(memberId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentId) {
        return paymentService.getPaymentById(paymentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{paymentId}")
    public ResponseEntity<?> updatePayment(@PathVariable String paymentId, @RequestBody Payment paymentDetails) {
        try {
            Optional<Payment> updatedPayment = paymentService.updatePaymentDetails(paymentId, paymentDetails);
            return updatedPayment.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment record " + paymentId + " not found."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating payment record.");
        }
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<?> deletePayment(@PathVariable String paymentId) {
        boolean deleted = paymentService.deletePayment(paymentId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment record " + paymentId + " not found.");
        }
    }

    @PostMapping("/update-overdue")
    public ResponseEntity<String> updateOverduePayments() {
        // Add Admin Role Check Here in a real app
        try {
            paymentService.updateOverduePayments();
            return ResponseEntity.ok("Overdue payment statuses updated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating overdue payments: " + e.getMessage());
        }
    }

    // Endpoint to manually trigger renewal invoice generation (for admin)
    @PostMapping("/admin/trigger-renewals")
    public ResponseEntity<String> triggerRenewalInvoiceGeneration(
            @RequestParam(defaultValue = "15") int daysInAdvance) {
        // IMPORTANT: Add security check to ensure only ADMIN can call this.
        // This is a simplified example without Spring Security.
        // In a real app: @PreAuthorize("hasRole('ADMIN')") or similar.
        try {
            paymentService.generateUpcomingRenewalInvoices(daysInAdvance);
            return ResponseEntity.ok("Renewal invoice generation process triggered for members due in " + daysInAdvance + " days.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error triggering renewal invoice generation: " + e.getMessage());
        }
    }
}