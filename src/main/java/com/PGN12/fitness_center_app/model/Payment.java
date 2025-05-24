package com.PGN12.fitness_center_app.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a payment or an invoice record.
 * For simplicity, we'll use this single class for both generated invoices and recorded payments.
 * The 'status' field will differentiate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    private String paymentId;// Unique ID for the payment/invoice (e.g., "INV001", "PAY001")
    private String memberId;
    private String memberName; // Denormalized for easier display
    private String planId; // ID of the membership plan this payment is for
    private String planName; // Denormalized plan name
    private String description; // e.g., "Monthly Subscription - Premium Plus", "Personal Training Session"
    private double amount;
    private String currency; // e.g., "USD", "LKR" - default to USD for now
    private String issueDate; // Date the invoice was generated or payment was due (ISO_LOCAL_DATE format)
    private String dueDate;   // Optional: Due date for invoices (ISO_LOCAL_DATE format)
    private String paymentDate; // Date the payment was made (ISO_LOCAL_DATE format), null if pending/overdue
    private String status; // "PENDING", "PAID", "OVERDUE", "CANCELLED"
    private String paymentMethod; // e.g., "Credit Card", "PayPal", "Cash" (for recording, not processing)
    private String transactionNotes; // Any additional notes for the transaction


}
