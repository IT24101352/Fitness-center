package com.PGN12.fitness_center_app.service;

import com.PGN12.fitness_center_app.dto.LoginResponse;
import com.PGN12.fitness_center_app.dto.RegistrationResponse;
import com.PGN12.fitness_center_app.model.*;
import com.PGN12.fitness_center_app.repository.MemberRepository;
import com.PGN12.fitness_center_app.repository.MembershipPlanRepository;
// Import PaymentRepository if deriving lastRenewalDate on the fly in this service
// import com.PGN12.fitness_center_app.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final PaymentService paymentService;
    // Uncomment if deriving renewal date here:
    // private final PaymentRepository paymentRepository;


    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    public MemberService(MemberRepository memberRepository,
                         MembershipPlanRepository planRepository,
                         PaymentService paymentService
            /* , PaymentRepository paymentRepository */) { // Add if needed
        this.memberRepository = memberRepository;
        this.planRepository = planRepository;
        this.paymentService = paymentService;
        // this.paymentRepository = paymentRepository; // Add if needed
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public Optional<Member> getMemberById(String id) {
        return memberRepository.findById(id);
    }

    public RegistrationResponse createMemberAndInitialInvoice(Member member) {
        if (member.getFullName() == null || member.getFullName().trim().isEmpty() ||
                member.getEmail() == null || member.getEmail().trim().isEmpty() ||
                member.getPassword() == null || member.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Full name, email, and password cannot be empty.");
        }
        if (memberRepository.findByEmail(member.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email '" + member.getEmail() + "' is already registered.");
        }
        if (member.getMembershipTierId() == null || member.getMembershipTierId().trim().isEmpty()) {
            throw new IllegalArgumentException("A membership tier must be selected.");
        }

        Optional<MembershipPlan> planOpt = planRepository.findById(member.getMembershipTierId());
        if (planOpt.isEmpty()) {
            throw new IllegalArgumentException("Selected membership plan (ID: " + member.getMembershipTierId() + ") not found.");
        }
        MembershipPlan selectedPlan = planOpt.get();

        if (member.getId() == null || member.getId().isEmpty()) {
            member.setId("MEM" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        if (member.getRole() == null || member.getRole().trim().isEmpty()) {
            member.setRole("MEMBER");
        }
        member.setStatus("PENDING_PAYMENT");
        String todayDate = LocalDate.now().format(DATE_FORMATTER);
        member.setJoinDate(todayDate);
        // For sorting consistency, initialize lastRenewalDate to joinDate for new members.
        // This will be updated upon first actual renewal.
        member.setLastRenewalDate(todayDate);


        Member createdMember = memberRepository.save(member);

        Payment initialInvoice = null;
        try {
            initialInvoice = paymentService.generateInvoice(
                    createdMember.getId(),
                    selectedPlan.getId(),
                    "Initial Membership Dues - " + selectedPlan.getName(),
                    selectedPlan.getPrice(),
                    "Rs.",
                    LocalDate.now()
            );
            System.out.println("Generated initial PENDING invoice " + initialInvoice.getPaymentId() + " for member " + createdMember.getId());
        } catch (Exception e) {
            System.err.println("CRITICAL: Member " + createdMember.getId() + " created, but failed to generate initial invoice: " + e.getMessage());
            throw new RuntimeException("User registered but failed to create initial invoice. Please contact support.", e);
        }

        return new RegistrationResponse(
                "Member account created. Please complete payment to activate.",
                createdMember.getId(),
                initialInvoice.getPaymentId(),
                createdMember
        );
    }

    public Optional<LoginResponse> loginMember(String email, String password) {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
                System.out.println("Login attempt for non-active member: " + email + " with status " + member.getStatus());
                if ("PENDING_PAYMENT".equalsIgnoreCase(member.getStatus())) {
                    // Find their pending invoice to redirect for payment
                    Optional<Payment> pendingInvoice = paymentService.getPaymentsByMemberId(member.getId())
                            .stream()
                            .filter(p -> "PENDING".equalsIgnoreCase(p.getStatus()) && p.getDescription() != null && p.getDescription().toLowerCase().contains("initial membership dues"))
                            .findFirst();
                    if (pendingInvoice.isPresent()) {
                        // Custom response or throw specific exception to indicate payment needed
                        // For now, returning empty will trigger "Invalid email or password, or account not active." in controller
                        System.out.println("Account for " + email + " is PENDING_PAYMENT. Invoice: " + pendingInvoice.get().getPaymentId());
                    }
                }
                return Optional.empty();
            }
            if (member.getPassword().equals(password)) {
                String token = UUID.randomUUID().toString();
                return Optional.of(new LoginResponse(
                        token,
                        member.getId(),
                        member.getFullName(),
                        member.getEmail(),
                        member.getRole(),
                        "Login successful."
                ));
            }
        }
        return Optional.empty();
    }

    public boolean activateMemberAccount(String memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            member.setStatus("ACTIVE");
            // Set lastRenewalDate to today if it's the very first activation after registration
            if (member.getLastRenewalDate() == null || member.getLastRenewalDate().isEmpty() || member.getLastRenewalDate().equals(member.getJoinDate())) {
                member.setLastRenewalDate(LocalDate.now().format(DATE_FORMATTER));
            }
            memberRepository.save(member);
            System.out.println("Member account " + memberId + " activated. Last renewal date set/confirmed: " + member.getLastRenewalDate());
            return true;
        }
        System.err.println("Attempted to activate non-existent member: " + memberId);
        return false;
    }

    public Optional<Member> updateMember(String id, Member memberDetails) {
        Optional<Member> existingMemberOpt = memberRepository.findById(id);
        if (existingMemberOpt.isPresent()) {
            Member existingMember = existingMemberOpt.get();
            if (memberDetails.getEmail() != null && !memberDetails.getEmail().equalsIgnoreCase(existingMember.getEmail())) {
                if (memberRepository.findByEmail(memberDetails.getEmail()).filter(m -> !m.getId().equals(id)).isPresent()) {
                    throw new IllegalArgumentException("Email '" + memberDetails.getEmail() + "' is already registered by another member.");
                }
                existingMember.setEmail(memberDetails.getEmail());
            }
            if (memberDetails.getFullName() != null) existingMember.setFullName(memberDetails.getFullName());
            if (memberDetails.getPassword() != null && !memberDetails.getPassword().isEmpty()) {
                existingMember.setPassword(memberDetails.getPassword());
            }
            if (memberDetails.getPhone() != null) existingMember.setPhone(memberDetails.getPhone());
            if (memberDetails.getDateOfBirth() != null) existingMember.setDateOfBirth(memberDetails.getDateOfBirth());

            // If membershipTierId changes, lastRenewalDate might need to be reset or handled
            // For simplicity, we assume lastRenewalDate is primarily updated by payment service
            if (memberDetails.getMembershipTierId() != null && !memberDetails.getMembershipTierId().equals(existingMember.getMembershipTierId())) {
                existingMember.setMembershipTierId(memberDetails.getMembershipTierId());
                // Consider if lastRenewalDate should be cleared or if a new initial payment for the new tier is required.
                // For now, changing tier via profile update doesn't automatically create an invoice or update renewal date.
                // That should ideally be part of an "upgrade/downgrade plan" flow that involves payment.
            }
            if (memberDetails.getRole() != null) existingMember.setRole(memberDetails.getRole());
            if (memberDetails.getStatus() != null) existingMember.setStatus(memberDetails.getStatus());
            if (memberDetails.getLastRenewalDate() != null) existingMember.setLastRenewalDate(memberDetails.getLastRenewalDate());


            return Optional.of(memberRepository.save(existingMember));
        }
        return Optional.empty();
    }

    public boolean deleteMember(String id) {
        return memberRepository.deleteById(id);
    }

    // --- Sorting Logic ---
    // Helper class for sorting
    private static class MemberRenewalInfo {
        private Member member;
        private LocalDate parsedRenewalDate;

        public MemberRenewalInfo(Member member, LocalDate parsedRenewalDate) {
            this.member = member;
            this.parsedRenewalDate = parsedRenewalDate;
        }
        public Member getMember() { return member; }
        public LocalDate getParsedRenewalDate() { return parsedRenewalDate; }
    }

    // Manual Insertion Sort Implementation
    private void manualInsertionSortMembersByRenewalDate(List<MemberRenewalInfo> list, boolean ascending) {
        int n = list.size();
        for (int i = 1; i < n; ++i) {
            MemberRenewalInfo keyInfo = list.get(i);
            LocalDate keyDate = keyInfo.getParsedRenewalDate(); // This is the date to sort by
            int j = i - 1;

            while (j >= 0) {
                LocalDate compareDate = list.get(j).getParsedRenewalDate();
                boolean shouldMove = false;

                // Simplified null handling: nulls are considered "earliest"
                if (keyDate == null && compareDate == null) { // both null, treat as equal
                    shouldMove = false;
                } else if (keyDate == null) { // key is null, compareDate is not
                    shouldMove = ascending; // if ascending, null (key) comes before non-null
                } else if (compareDate == null) { // key is not null, compareDate is null
                    shouldMove = !ascending; // if ascending, non-null (key) comes after null
                } else { // Both dates non-null
                    shouldMove = ascending ? keyDate.isBefore(compareDate) : keyDate.isAfter(compareDate);
                }

                if (shouldMove) {
                    list.set(j + 1, list.get(j));
                    j = j - 1;
                } else {
                    break;
                }
            }
            list.set(j + 1, keyInfo);
        }
    }


    public List<Member> getMembersSortedByLastRenewalDate(boolean ascending) {
        List<Member> members = memberRepository.findAll();
        List<MemberRenewalInfo> sortableMembers = new ArrayList<>();

        for (Member member : members) {
            LocalDate renewalDate = null;
            if (member.getLastRenewalDate() != null && !member.getLastRenewalDate().isEmpty()) {
                try {
                    renewalDate = LocalDate.parse(member.getLastRenewalDate(), DATE_FORMATTER);
                } catch (Exception e) {
                    System.err.println("Could not parse lastRenewalDate for member " + member.getId() + ": " + member.getLastRenewalDate() + ". Using fallback MIN_DATE.");
                    renewalDate = LocalDate.MIN; // Fallback for unparseable dates
                }
            } else {
                // If no lastRenewalDate, use a very old date for sorting comparison (nulls first/last strategy)
                renewalDate = LocalDate.MIN;
            }
            sortableMembers.add(new MemberRenewalInfo(member, renewalDate));
        }

        // Perform your manual insertion sort
        manualInsertionSortMembersByRenewalDate(sortableMembers, ascending);

        return sortableMembers.stream()
                .map(MemberRenewalInfo::getMember)
                .collect(Collectors.toList());
    }
}