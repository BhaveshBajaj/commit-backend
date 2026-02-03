package com.commit.commit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commit.commit.entity.ApproverStatus;
import com.commit.commit.entity.CommitmentApprover;

public interface CommitmentApproverRepository extends JpaRepository<CommitmentApprover, Long> {
    List<CommitmentApprover> findByCommitmentId(Long commitmentId);
    
    @Query("SELECT ca FROM CommitmentApprover ca JOIN FETCH ca.user WHERE ca.commitment.id = :commitmentId")
    List<CommitmentApprover> findByCommitmentIdWithUser(@Param("commitmentId") Long commitmentId);
    Optional<CommitmentApprover> findByCommitmentIdAndUserId(Long commitmentId, Long userId);
    boolean existsByCommitmentIdAndStatusNot(Long commitmentId, ApproverStatus status);
}
