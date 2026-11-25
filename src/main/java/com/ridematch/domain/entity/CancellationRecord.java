package com.ridematch.domain.entity;

import java.math.BigDecimal;
import java.util.Objects;

import org.hibernate.annotations.Check;

import com.ridematch.domain.enums.CancellationOutcome;
import com.ridematch.domain.enums.CancellationPolicyDecision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Audit record for a cancelled participation; it never represents a collected penalty. */
@Entity
@Table(name = "cancellation_records", indexes = {
        @Index(name = "idx_cancellation_records_membership", columnList = "ride_membership_id"),
        @Index(name = "idx_cancellation_records_outcome", columnList = "outcome")
})
@Check(name = "ck_cancellation_loss_nonnegative", constraints = "actual_financial_loss IS NULL OR actual_financial_loss >= 0")
public class CancellationRecord extends AuditableEntity {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_membership_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cancellation_records_membership"))
    private RideMembership rideMembership;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CancellationOutcome outcome = CancellationOutcome.PENDING_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_decision", nullable = false, length = 30)
    private CancellationPolicyDecision policyDecision = CancellationPolicyDecision.NO_POTENTIAL_LOSS;

    @Column(name = "policy_explanation", length = 500)
    private String policyExplanation;

    @Column(name = "actual_financial_loss", precision = 10, scale = 2)
    private BigDecimal actualFinancialLoss;

    protected CancellationRecord() {
    }

    public CancellationRecord(RideMembership rideMembership, String reason) {
        this(rideMembership, reason, CancellationPolicyDecision.NO_POTENTIAL_LOSS,
                "No potential financial loss was identified by the configured policy.");
    }

    public CancellationRecord(RideMembership rideMembership, String reason,
            CancellationPolicyDecision policyDecision, String policyExplanation) {
        this.rideMembership = Objects.requireNonNull(rideMembership, "rideMembership must not be null");
        this.reason = normalizeReason(reason);
        this.policyDecision = Objects.requireNonNull(policyDecision, "policyDecision must not be null");
        this.policyExplanation = policyExplanation == null ? null : policyExplanation.trim();
        this.outcome = policyDecision == CancellationPolicyDecision.NO_POTENTIAL_LOSS
                ? CancellationOutcome.NO_FINANCIAL_LOSS : CancellationOutcome.PENDING_REVIEW;
    }

    public void recordNoFinancialLoss() {
        this.outcome = CancellationOutcome.NO_FINANCIAL_LOSS;
        this.actualFinancialLoss = null;
    }

    public void recordActualFinancialLoss(BigDecimal actualFinancialLoss) {
        Objects.requireNonNull(actualFinancialLoss, "actualFinancialLoss must not be null");
        if (actualFinancialLoss.signum() < 0) {
            throw new IllegalArgumentException("actualFinancialLoss must not be negative");
        }
        this.actualFinancialLoss = actualFinancialLoss;
        this.outcome = CancellationOutcome.ACTUAL_LOSS_RECORDED;
    }

    public Long getId() { return id; }
    public RideMembership getRideMembership() { return rideMembership; }
    public String getReason() { return reason; }
    public CancellationOutcome getOutcome() { return outcome; }
    public CancellationPolicyDecision getPolicyDecision() { return policyDecision; }
    public String getPolicyExplanation() { return policyExplanation; }
    public BigDecimal getActualFinancialLoss() { return actualFinancialLoss; }

    private static String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }
}
