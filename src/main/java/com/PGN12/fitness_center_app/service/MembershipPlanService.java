package com.PGN12.fitness_center_app.service;

import com.PGN12.fitness_center_app.model.MembershipPlan;
import com.PGN12.fitness_center_app.repository.MembershipPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for MembershipPlan business logic.
 */
@Service
public class MembershipPlanService {

    private final MembershipPlanRepository planRepository;

    @Autowired
    public MembershipPlanService(MembershipPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<MembershipPlan> getAllPlans() {
        return planRepository.findAll();
    }

    public Optional<MembershipPlan> getPlanById(String id) {
        return planRepository.findById(id);
    }

    public MembershipPlan createPlan(MembershipPlan plan) {
        // Basic validation
        if (plan.getName() == null || plan.getName().trim().isEmpty() || plan.getPrice() <= 0) {
            throw new IllegalArgumentException("Plan name cannot be empty and price must be positive.");
        }

        // Generate unique ID if not provided
        if (plan.getId() == null || plan.getId().isEmpty()) {
            plan.setId("PLAN" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        if (plan.getStatus() == null || plan.getStatus().isEmpty()){
            plan.setStatus("active"); // Default to active
        }
        return planRepository.save(plan);
    }

    public Optional<MembershipPlan> updatePlan(String id, MembershipPlan planDetails) {
        Optional<MembershipPlan> existingPlanOpt = planRepository.findById(id);
        if (existingPlanOpt.isPresent()) {
            MembershipPlan existingPlan = existingPlanOpt.get();
            if (planDetails.getName() != null) {
                existingPlan.setName(planDetails.getName());
            }
            if (planDetails.getPrice() > 0) {
                existingPlan.setPrice(planDetails.getPrice());
            }
            if (planDetails.getDuration() != null) {
                existingPlan.setDuration(planDetails.getDuration());
            }
            if (planDetails.getDescription() != null) {
                existingPlan.setDescription(planDetails.getDescription());
            }
            if (planDetails.getStatus() != null) {
                existingPlan.setStatus(planDetails.getStatus());
            }
            return Optional.of(planRepository.save(existingPlan));
        }
        return Optional.empty(); // Plan not found
    }

    public boolean deletePlan(String id) {
        return planRepository.deleteById(id);
    }
}
