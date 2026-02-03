package com.commit.commit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commit.commit.entity.CommitmentEvent;

public interface CommitmentEventRepository extends JpaRepository<CommitmentEvent, Long> {
    List<CommitmentEvent> findByCommitmentIdOrderByCreatedAtAsc(Long commitmentId);
}
