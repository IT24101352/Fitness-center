package com.PGN12.fitness_center_app.service;


import com.PGN12.fitness_center_app.model.Member;
import com.PGN12.fitness_center_app.model.MembershipPlan;
import com.PGN12.fitness_center_app.model.Payment;
import com.PGN12.fitness_center_app.repository.MemberRepository;
import com.PGN12.fitness_center_app.repository.MembershipPlanRepository;
import com.PGN12.fitness_center_app.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy; // <-- IMPORT @Lazy

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final MemberService memberService; // This is part of the cycle

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    // Add @Lazy to the MemberService parameter in the constructor
    public PaymentService(PaymentRepository paymentRepository,
                          MemberRepository memberRepository,
                          MembershipPlanRepository planRepository,
                          @Lazy MemberService memberService) { // <-- ADD @Lazy HERE
        this.paymentRepository = paymentRepository;
        this.memberRepository = memberRepository;
        this.planRepository = planRepository;
        this.memberService = memberService;
    }

    // ... rest of your PaymentService code ...

    public Payment generateInvoice(String memberId, String planId, String description, double amount, String currency, LocalDate dueDate) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("Member with ID " + memberId + " not found.");
        }
        Member member = memberOpt.get();

        MembershipPlan plan = null;
        if (planId != null && !planId.trim().isEmpty()) {
            Optional<MembershipPlan> planOpt = planRepository.findById(planId);
            if (planOpt.isEmpty()) {
                throw new IllegalArgumentException("Membership Plan with ID " + planId + " not found.");
            }
            plan = planOpt.get();
        }

        Payment invoice = new Payment();
        invoice.setPaymentId("INV" + UUID.randomUUID().toString().substring(0, 7).toUpperCase());
        invoice.setMemberId(member.getId());
        invoice.setMemberName(member.getFullName());

        if (plan != null) {
            invoice.setPlanId(plan.getId());
            invoice.setPlanName(plan.getName());
            invoice.setAmount(amount > 0 ? amount : plan.getPrice());
            invoice.setDescription(description != null ? description : "Membership Dues - " + plan.getName());
        } else {
            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive for manual invoices.");
            if (description == null || description.trim().isEmpty()) throw new IllegalArgumentException("Description is required for manual invoices.");
            invoice.setAmount(amount);
            invoice.setDescription(description);
        }

        invoice.setCurrency(currency != null ? currency : "Rs.");
        invoice.setIssueDate(LocalDate.now().format(DATE_FORMATTER));
        invoice.setDueDate(dueDate != null ? dueDate.format(DATE_FORMATTER) : LocalDate.now().plusDays(15).format(DATE_FORMATTER));
        invoice.setStatus("PENDING");

        return paymentRepository.save(invoice);
    }

    public Payment processPayment(String paymentId, String paymentMethod, double paymentAmount) {
        Optional<Payment> invoiceOpt = paymentRepository.findById(paymentId);
        if (invoiceOpt.isEmpty()) {
            throw new IllegalArgumentException("Invoice with ID " + paymentId + " not found.");
        }
        Payment invoice = invoiceOpt.get();

        if (!"PENDING".equalsIgnoreCase(invoice.getStatus()) && !"OVERDUE".equalsIgnoreCase(invoice.getStatus())) {
            throw new IllegalArgumentException("Invoice " + paymentId + " is not in PENDING or OVERDUE status. Current status: " + invoice.getStatus());
        }

        if (invoice.getAmount() != paymentAmount) {
            System.err.println("Warning: Payment amount " + paymentAmount + " for invoice " + paymentId +
                    " does not match invoice amount " + invoice.getAmount() + ". Processing anyway for this system.");
        }

        invoice.setStatus("PAID");
        invoice.setPaymentDate(LocalDate.now().format(DATE_FORMATTER));
        invoice.setPaymentMethod(paymentMethod);
        invoice.setTransactionNotes("Payment of $" + paymentAmount + " received on " + invoice.getPaymentDate() + " via " + paymentMethod);
        Payment savedPayment = paymentRepository.save(invoice);

        Optional<Member> memberOpt = memberRepository.findById(invoice.getMemberId());
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            if ("PENDING_PAYMENT".equalsIgnoreCase(member.getStatus()) &&
                    (paymentMethod.equals("INITIAL_REGISTRATION_PAYMENT") ||
                            (invoice.getDescription() != null && invoice.getDescription().toLowerCase().contains("initial membership dues")))) {
                boolean isFirstMembershipPayment = paymentRepository.findByMemberId(member.getId()).stream()
                        .filter(p -> "PAID".equalsIgnoreCase(p.getStatus()) &&
                                (p.getDescription() != null && p.getDescription().toLowerCase().contains("membership dues")))
                        .count() == 1;

                if(isFirstMembershipPayment){
                    memberService.activateMemberAccount(member.getId());
                }
            }
        }
        return savedPayment;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsByMemberId(String memberId) {
        return paymentRepository.findByMemberId(memberId);
    }

    public Optional<Payment> getPaymentById(String paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public Optional<Payment> updatePaymentDetails(String paymentId, Payment paymentDetails) {
        Optional<Payment> existingOpt = paymentRepository.findById(paymentId);
        if (existingOpt.isPresent()) {
            Payment existing = existingOpt.get();
            if (paymentDetails.getStatus() != null) existing.setStatus(paymentDetails.getStatus());
            if (paymentDetails.getTransactionNotes() != null) existing.setTransactionNotes(paymentDetails.getTransactionNotes());
            if (paymentDetails.getAmount() != existing.getAmount() && "PENDING".equalsIgnoreCase(existing.getStatus())) {
                existing.setAmount(paymentDetails.getAmount());
            }
            if (paymentDetails.getDueDate() != null) existing.setDueDate(paymentDetails.getDueDate());
            if (paymentDetails.getPaymentDate() != null && "PAID".equalsIgnoreCase(paymentDetails.getStatus())) {
                existing.setPaymentDate(paymentDetails.getPaymentDate());
            }
            if (paymentDetails.getPaymentMethod() != null && "PAID".equalsIgnoreCase(paymentDetails.getStatus())) {
                existing.setPaymentMethod(paymentDetails.getPaymentMethod());
            }
            return Optional.of(paymentRepository.save(existing));
        }
        return Optional.empty();
    }

    public boolean deletePayment(String paymentId) {
        return paymentRepository.deleteById(paymentId);
    }

    public void updateOverduePayments() {
        List<Payment> pendingPayments = paymentRepository.findAll().stream()
                .filter(p -> "PENDING".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        for (Payment payment : pendingPayments) {
            if (payment.getDueDate() != null) {
                try {
                    LocalDate dueDate = LocalDate.parse(payment.getDueDate(), DATE_FORMATTER);
                    if (today.isAfter(dueDate)) {
                        payment.setStatus("OVERDUE");
                        paymentRepository.save(payment);
                        System.out.println("Invoice " + payment.getPaymentId() + " for member " + payment.getMemberId() + " is now OVERDUE.");
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing due date for payment " + payment.getPaymentId() + ": " + payment.getDueDate());
                }
            }
        }
    }
}