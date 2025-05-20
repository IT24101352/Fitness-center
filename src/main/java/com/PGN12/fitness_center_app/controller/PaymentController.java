package com.PGN12.fitness_center_app.controller;

import com.PGN12.fitness_center_app.model.Payment;
import com.PGN12.fitness_center_app.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for managing Payments and Billing.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Generates a new invoice (Admin operation).
     * Expects memberId, planId, description, amount, currency, dueDate in payload.
     * Endpoint: POST /api/payments/generate-invoice
     */
    @PostMapping("/generate-invoice")
    public ResponseEntity<?> generateInvoice(@RequestBody Map<String, String> payload) {
        try {
            String memberId = payload.get("memberId");
            String planId = payload.get("planId");
            String description = payload.get("description");
            double amount = Double.parseDouble(payload.getOrDefault("amount", "0"));
            String currency = payload.getOrDefault("currency", "USD");
            LocalDate dueDate = payload.containsKey("dueDate") ? LocalDate.parse(payload.get("dueDate"), DateTimeFormatter.ISO_LOCAL_DATE) : null;

            if (memberId == null || planId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("memberId and planId are required.");
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

    /**
     * Processes a payment for a given invoice ID (Member operation from payment portal).
     * Expects paymentMethod and paymentAmount.
     * Endpoint: POST /api/payments/{paymentId}/pay
     */
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


    /**
     * Retrieves all payment records (Admin view).
     * Can be filtered by status.
     * Endpoint: GET /api/payments
     */
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

    /**
     * Retrieves payment history for a specific member.
     * Endpoint: GET /api/payments/member/{memberId}
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<Payment>> getPaymentsByMemberId(@PathVariable String memberId) {
        List<Payment> payments = paymentService.getPaymentsByMemberId(memberId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Retrieves a specific payment/invoice by its ID.
     * Endpoint: GET /api/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentId) {
        return paymentService.getPaymentById(paymentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Updates details of a payment/invoice (Admin operation).
     * Endpoint: PUT /api/payments/{paymentId}
     */
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

    /**
     * Deletes a payment/invoice record (Admin operation).
     * Endpoint: DELETE /api/payments/{paymentId}
     */
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<?> deletePayment(@PathVariable String paymentId) {
        boolean deleted = paymentService.deletePayment(paymentId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment record " + paymentId + " not found.");
        }
    }

    /**
     * Endpoint to trigger updating overdue payment statuses (Admin/Scheduled Task).
     * Endpoint: POST /api/payments/update-overdue
     */
    @PostMapping("/update-overdue")
    public ResponseEntity<String> updateOverduePayments() {
        try {
            paymentService.updateOverduePayments();
            return ResponseEntity.ok("Overdue payment statuses updated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating overdue payments: " + e.getMessage());
        }
    }
}
