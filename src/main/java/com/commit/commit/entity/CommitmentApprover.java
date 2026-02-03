package com.commit.commit.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "commitment_approvers", uniqueConstraints = @UniqueConstraint(columnNames = {"commitment_id", "user_id"}))
public class CommitmentApprover {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commitment_id")
    private Commitment commitment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApproverStatus status;

    @Column(name = "acted_at")
    private OffsetDateTime actedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Commitment getCommitment() { return commitment; }
    public void setCommitment(Commitment commitment) { this.commitment = commitment; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public ApproverStatus getStatus() { return status; }
    public void setStatus(ApproverStatus status) { this.status = status; }
    public OffsetDateTime getActedAt() { return actedAt; }
    public void setActedAt(OffsetDateTime actedAt) { this.actedAt = actedAt; }
}
