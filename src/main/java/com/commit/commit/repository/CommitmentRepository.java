package com.commit.commit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commit.commit.entity.Commitment;
import com.commit.commit.entity.CommitmentStatus;

public interface CommitmentRepository extends JpaRepository<Commitment, Long> {
    List<Commitment> findBySpaceId(Long spaceId);
    int countBySpaceId(Long spaceId);
    
    boolean existsBySpaceIdAndCreatedByIdAndStatus(Long spaceId, Long userId, CommitmentStatus status);
}
