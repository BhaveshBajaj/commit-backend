package com.commit.commit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commit.commit.entity.Commitment;
import com.commit.commit.entity.CommitmentStatus;

public interface CommitmentRepository extends JpaRepository<Commitment, Long> {
    List<Commitment> findBySpaceId(Long spaceId);
    
    @Query("SELECT c FROM Commitment c JOIN FETCH c.space JOIN FETCH c.createdBy WHERE c.space.id = :spaceId")
    List<Commitment> findBySpaceIdWithDetails(@Param("spaceId") Long spaceId);
    
    int countBySpaceId(Long spaceId);
    
    boolean existsBySpaceIdAndCreatedByIdAndStatus(Long spaceId, Long userId, CommitmentStatus status);
}
