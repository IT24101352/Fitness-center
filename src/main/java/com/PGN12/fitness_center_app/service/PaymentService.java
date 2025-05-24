package com.PGN12.fitness_center_app.service;

import com.PGN12.fitness_center_app.model.Member;
import com.PGN12.fitness_center_app.model.MembershipPlan;
import com.PGN12.fitness_center_app.model.Payment;
import com.PGN12.fitness_center_app.repository.MemberRepository;
import com.PGN12.fitness_center_app.repository.MembershipPlanRepository;
import com.PGN12.fitness_center_app.repository.PaymentRepository;
import com.PGN12.fitness_center_app.ds.ManualRenewalQueue; // Import manual queue
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
// To use @Scheduled, you would need:
// import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;// Used to handle dates like issueDate, dueDate, paymentDate
import java.time.format.DateTimeFormatter; // Used to format LocalDate to String and parse back
import java.util.Comparator;// Used to sort collections like payment lists by custom rules
import java.util.List;// Used to store collections of data like List<Payment>, List<Member>
import java.util.Optional; // Used to s
// afely handle values that might be null (e.g., findById)
import java.util.UUID; //generates unique ID s
import java.util.stream.Collectors; // Used to collect results from Stream operations

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final MemberService memberService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          MemberRepository memberRepository,
                          MembershipPlanRepository planRepository,
                          @Lazy MemberService memberService) {// initialize a bean lazily,only when it is actually
        this.paymentRepository = paymentRepository;
        this.memberRepository = memberRepository;
        this.planRepository = planRepository;
        this.memberService = memberService;
    }

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
            invoice.setAmount(amount > 0 ? amount : plan.getPrice()); // Use provided amount if > 0, else plan price
            invoice.setDescription(description != null ? description : "Membership Dues - " + plan.getName());
        } else {
            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive for manual invoices without a plan.");
            if (description == null || description.trim().isEmpty()) throw new IllegalArgumentException("Description is required for manual invoices without a plan.");
            invoice.setAmount(amount);
            invoice.setDescription(description);
        }

        invoice.setCurrency(currency != null ? currency : "Rs."); // Default to "Rs." if not provided
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

        if (invoice.getAmount() != paymentAmount && !"INITIAL_REGISTRATION_PAYMENT".equals(paymentMethod)) {
            System.err.println("Warning: Payment amount " + paymentAmount + " for invoice " + paymentId +
                    " does not match invoice amount " + invoice.getAmount() + ". Processing with invoice amount for this system if not initial.");
        }

        invoice.setStatus("PAID");
        invoice.setPaymentDate(LocalDate.now().format(DATE_FORMATTER));
        invoice.setPaymentMethod(paymentMethod);
        invoice.setTransactionNotes("Payment of Rs." + invoice.getAmount() + " received on " + invoice.getPaymentDate() + " via " + paymentMethod);
        Payment savedPayment = paymentRepository.save(invoice);

        Optional<Member> memberOpt = memberRepository.findById(invoice.getMemberId());
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            boolean isInitialPayment = "INITIAL_REGISTRATION_PAYMENT".equals(paymentMethod) ||
                    (invoice.getDescription() != null && invoice.getDescription().toLowerCase().contains("initial membership dues"));
            boolean isRenewalPayment = invoice.getDescription() != null && invoice.getDescription().toLowerCase().contains("renewal");

            if (isInitialPayment && "PENDING_PAYMENT".equalsIgnoreCase(member.getStatus())) {
                memberService.activateMemberAccount(member.getId());
                member.setLastRenewalDate(savedPayment.getPaymentDate()); // Also set for initial for consistency
                memberRepository.save(member);
            } else if (isRenewalPayment) {
                if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
                    member.setStatus("ACTIVE");
                }
                member.setLastRenewalDate(savedPayment.getPaymentDate());
                memberRepository.save(member);
                System.out.println("Updated lastRenewalDate for member " + member.getId() + " to " + savedPayment.getPaymentDate() + " after renewal.");
            }
        }
        return savedPayment;
    }

    private LocalDate calculateExpiryDate(LocalDate startDate, String duration) {
        if (startDate == null || duration == null || duration.trim().isEmpty()) {
            System.err.println("Cannot calculate expiry: start date or duration is null/empty. StartDate: " + startDate + ", Duration: " + duration);
            return null;
        }
        duration = duration.toLowerCase();
        if (duration.equals("monthly")) {
            return startDate.plusMonths(1);
        } else if (duration.equals("yearly")) {
            return startDate.plusYears(1);
        } else if (duration.contains("months")) {
            try {
                int numMonths = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
                return startDate.plusMonths(numMonths);
            } catch (NumberFormatException e) {
                System.err.println("Could not parse month duration: " + duration);
                return null;
            }
        }
        System.err.println("Unhandled duration type for expiry calculation: " + duration);
        return null;
    }

    public void generateUpcomingRenewalInvoices(int daysBeforeExpiryThreshold) {
        System.out.println("Starting process to generate upcoming renewal invoices...");
        List<Member> activeMembers = memberRepository.findAll().stream()
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()) && m.getMembershipTierId() != null)
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        ManualRenewalQueue renewalQueue = new ManualRenewalQueue();

        for (Member member : activeMembers) {
            Optional<MembershipPlan> memberPlanOpt = planRepository.findById(member.getMembershipTierId());
            if (memberPlanOpt.isEmpty()) {
                System.err.println("Skipping renewal for member " + member.getId() + ": Plan " + member.getMembershipTierId() + " not found.");
                continue;
            }
            MembershipPlan memberPlan = memberPlanOpt.get();

            LocalDate referenceDateForRenewal = null;
            if (member.getLastRenewalDate() != null && !member.getLastRenewalDate().isEmpty()) {
                try {
                    referenceDateForRenewal = LocalDate.parse(member.getLastRenewalDate(), DATE_FORMATTER);
                } catch (Exception e) {
                    System.err.println("Error parsing lastRenewalDate for member " + member.getId() + ": " + member.getLastRenewalDate() + ". Skipping this member for renewal check.");
                    continue;
                }
            } else if (member.getJoinDate() != null && !member.getJoinDate().isEmpty()) {
                try {
                    referenceDateForRenewal = LocalDate.parse(member.getJoinDate(), DATE_FORMATTER);
                } catch (Exception e) {
                    System.err.println("Error parsing joinDate for member " + member.getId() + ": " + member.getJoinDate() + ". Skipping this member for renewal check.");
                    continue;
                }
            }


            if (referenceDateForRenewal == null) {
                System.err.println("Skipping renewal for member " + member.getId() + ": No valid last renewal or join date.");
                continue;
            }

            LocalDate expiryDate = calculateExpiryDate(referenceDateForRenewal, memberPlan.getDuration());
            if (expiryDate == null) {
                System.err.println("Skipping renewal for member " + member.getId() + ": Could not calculate expiry date for plan duration '" + memberPlan.getDuration() + "'.");
                continue;
            }

            boolean pendingRenewalExists = paymentRepository.findByMemberId(member.getId()).stream()
                    .anyMatch(p -> "PENDING".equalsIgnoreCase(p.getStatus()) &&
                            memberPlan.getId().equals(p.getPlanId()) &&
                            p.getDescription() != null && p.getDescription().toLowerCase().contains("renewal"));

            if (pendingRenewalExists) {
                continue;
            }

            if (expiryDate.isBefore(today.plusDays(daysBeforeExpiryThreshold)) || expiryDate.isEqual(today.plusDays(daysBeforeExpiryThreshold))) {
                System.out.println("Adding member to renewal queue: " + member.getFullName() + " (Plan: " + memberPlan.getName() + ", Expiry: " + expiryDate + ")");
                renewalQueue.enqueue(member);
            }
        }

        System.out.println("Processing renewal queue. Items to process: " + renewalQueue.size());
        while (!renewalQueue.isEmpty()) {
            Member memberToProcess = renewalQueue.dequeue();
            MembershipPlan planToRenew = planRepository.findById(memberToProcess.getMembershipTierId()).orElse(null);

            if (planToRenew != null) {
                System.out.println("Generating renewal invoice from queue for: " + memberToProcess.getFullName() + " - Plan: " + planToRenew.getName());
                try {
                    LocalDate dueDateForRenewal = LocalDate.now().plusDays(7);
                    generateInvoice(
                            memberToProcess.getId(),
                            planToRenew.getId(),
                            "Membership Renewal - " + planToRenew.getName(),
                            planToRenew.getPrice(),
                            "Rs.", // FIX: Directly pass "Rs." or your default currency
                            dueDateForRenewal
                    );
                } catch (Exception e) {
                    System.err.println("Error generating renewal invoice for member " + memberToProcess.getId() + " from queue: " + e.getMessage());
                }
            } else {
                System.err.println("Could not find plan details for member in queue: " + memberToProcess.getId());
            }
        }
        System.out.println("Finished processing renewal invoices.");
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
            if (paymentDetails.getAmount() != 0 && paymentDetails.getAmount() != existing.getAmount() && "PENDING".equalsIgnoreCase(existing.getStatus())) {
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
            if (payment.getDueDate() != null && !payment.getDueDate().isEmpty()) {
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
