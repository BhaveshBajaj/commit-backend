package com.commit.commit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commit.commit.entity.ApproverStatus;
import com.commit.commit.entity.CommitmentApprover;

public interface CommitmentApproverRepository extends JpaRepository<CommitmentApprover, Long> {
    List<CommitmentApprover> findByCommitmentId(Long commitmentId);
    Optional<CommitmentApprover> findByCommitmentIdAndUserId(Long commitmentId, Long userId);
    boolean existsByCommitmentIdAndStatusNot(Long commitmentId, ApproverStatus status);
}
