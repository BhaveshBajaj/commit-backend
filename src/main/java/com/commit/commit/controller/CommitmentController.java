package com.commit.commit.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.commit.commit.dto.CommitmentHistoryResponse;
import com.commit.commit.dto.CommitmentResponse;
import com.commit.commit.dto.CreateCommitmentRequest;
import com.commit.commit.dto.UpdateCommitmentRequest;
import com.commit.commit.security.AuthenticatedUser;
import com.commit.commit.service.CommitmentService;

import jakarta.validation.Valid;

@RestController
public class CommitmentController {
    private final CommitmentService commitmentService;

    public CommitmentController(CommitmentService commitmentService) {
        this.commitmentService = commitmentService;
    }

    @PostMapping("/spaces/{spaceId}/commitments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommitmentResponse createCommitment(
            @PathVariable Long spaceId,
            @Valid @RequestBody CreateCommitmentRequest request) {
        return commitmentService.createCommitment(AuthenticatedUser.getUserId(), spaceId, request);
    }

    @PutMapping("/commitments/{id}")
    public CommitmentResponse updateCommitment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommitmentRequest request) {
        return commitmentService.updateCommitment(AuthenticatedUser.getUserId(), id, request);
    }

    @PostMapping("/commitments/{id}/review")
    public CommitmentResponse sendForReview(@PathVariable Long id) {
        return commitmentService.sendForReview(AuthenticatedUser.getUserId(), id);
    }

    @PostMapping("/commitments/{id}/approve")
    public CommitmentResponse approve(@PathVariable Long id) {
        return commitmentService.approve(AuthenticatedUser.getUserId(), id);
    }

    @PostMapping("/commitments/{id}/reject")
    public CommitmentResponse reject(@PathVariable Long id) {
        return commitmentService.reject(AuthenticatedUser.getUserId(), id);
    }

    @GetMapping("/spaces/{spaceId}/commitments")
    public java.util.List<CommitmentResponse> getSpaceCommitments(@PathVariable Long spaceId) {
        return commitmentService.getSpaceCommitments(AuthenticatedUser.getUserId(), spaceId);
    }

    @GetMapping("/commitments/{id}")
    public CommitmentResponse getCommitment(@PathVariable Long id) {
        return commitmentService.getCommitmentById(AuthenticatedUser.getUserId(), id);
    }

    @GetMapping("/commitments/{id}/history")
    public java.util.List<CommitmentHistoryResponse> getCommitmentHistory(@PathVariable Long id) {
        return commitmentService.getCommitmentHistory(AuthenticatedUser.getUserId(), id);
    }
}
