package com.commit.commit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commit.commit.entity.CommitmentEvent;

public interface CommitmentEventRepository extends JpaRepository<CommitmentEvent, Long> {
    List<CommitmentEvent> findByCommitmentIdOrderByCreatedAtAsc(Long commitmentId);
    
    @Query("SELECT ce FROM CommitmentEvent ce JOIN FETCH ce.actor WHERE ce.commitment.id = :commitmentId ORDER BY ce.createdAt ASC")
    List<CommitmentEvent> findByCommitmentIdWithActorOrderByCreatedAtAsc(@Param("commitmentId") Long commitmentId);
}
