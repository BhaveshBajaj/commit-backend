package com.commit.commit.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commit.commit.dto.ApproverResponse;
import com.commit.commit.dto.CommitmentHistoryResponse;
import com.commit.commit.dto.CommitmentResponse;
import com.commit.commit.dto.CreateCommitmentRequest;
import com.commit.commit.dto.PerformedByResponse;
import com.commit.commit.dto.UpdateCommitmentRequest;
import com.commit.commit.entity.ApproverStatus;
import com.commit.commit.entity.Commitment;
import com.commit.commit.entity.CommitmentApprover;
import com.commit.commit.entity.CommitmentEvent;
import com.commit.commit.entity.CommitmentStatus;
import com.commit.commit.entity.MembershipStatus;
import com.commit.commit.entity.Space;
import com.commit.commit.entity.User;
import com.commit.commit.exception.InvalidStateException;
import com.commit.commit.exception.NotFoundException;
import com.commit.commit.exception.UnauthorizedException;
import com.commit.commit.repository.CommitmentApproverRepository;
import com.commit.commit.repository.CommitmentEventRepository;
import com.commit.commit.repository.CommitmentRepository;
import com.commit.commit.repository.SpaceRepository;
import com.commit.commit.repository.UserRepository;
import com.commit.commit.repository.UserSpaceRepository;

@Service
public class CommitmentService {
    private final CommitmentRepository commitmentRepository;
    private final CommitmentApproverRepository approverRepository;
    private final CommitmentEventRepository eventRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final UserSpaceRepository userSpaceRepository;

    public CommitmentService(CommitmentRepository commitmentRepository,
                             CommitmentApproverRepository approverRepository,
                             CommitmentEventRepository eventRepository,
                             SpaceRepository spaceRepository,
                             UserRepository userRepository,
                             UserSpaceRepository userSpaceRepository) {
        this.commitmentRepository = commitmentRepository;
        this.approverRepository = approverRepository;
        this.eventRepository = eventRepository;
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
        this.userSpaceRepository = userSpaceRepository;
    }

    @Transactional
    public CommitmentResponse createCommitment(Long userId, Long spaceId, CreateCommitmentRequest request) {
        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        Space space = spaceRepository.findById(spaceId)
            .orElseThrow(() -> new NotFoundException("Space not found"));

        if (!userSpaceRepository.existsByUserIdAndSpaceIdAndStatus(userId, spaceId, MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("User not member of space");
        }

        Set<Long> approverIds = new HashSet<>(request.approverIds());
        approverIds.add(userId); // creator is implicitly an approver

        List<User> approvers = userRepository.findAllByIdIn(new ArrayList<>(approverIds));
        if (approvers.size() != approverIds.size()) {
            throw new NotFoundException("One or more approvers not found");
        }

        for (User approver : approvers) {
            if (!userSpaceRepository.existsByUserIdAndSpaceIdAndStatus(approver.getId(), spaceId, MembershipStatus.APPROVED)) {
                throw new InvalidStateException("Approver " + approver.getId() + " not member of space");
            }
        }

        Commitment commitment = new Commitment();
        commitment.setSpace(space);
        commitment.setTitle(request.title());
        commitment.setDescription(request.description());
        commitment.setDeadline(toOffsetDateTime(request.deadline()));
        commitment.setStatus(CommitmentStatus.DRAFT);
        commitment.setCreatedBy(creator);
        commitment.setCreatedAt(OffsetDateTime.now());
        commitment = commitmentRepository.save(commitment);

        for (User approver : approvers) {
            CommitmentApprover ca = new CommitmentApprover();
            ca.setCommitment(commitment);
            ca.setUser(approver);
            ca.setStatus(ApproverStatus.PENDING);
            approverRepository.save(ca);
        }

        emitEvent(commitment, creator, "CREATED", null);
        return toResponse(commitment);
    }

    @Transactional
    public CommitmentResponse updateCommitment(Long userId, Long commitmentId, UpdateCommitmentRequest request) {
        Commitment commitment = getCommitment(commitmentId);
        User actor = getUser(userId);

        if (commitment.getStatus() != CommitmentStatus.DRAFT) {
            throw new InvalidStateException("Can only edit DRAFT commitments");
        }

        if (request.title() != null) commitment.setTitle(request.title());
        if (request.description() != null) commitment.setDescription(request.description());
        if (request.deadline() != null) commitment.setDeadline(toOffsetDateTime(request.deadline()));

        commitment = commitmentRepository.save(commitment);
        emitEvent(commitment, actor, "EDITED", null);
        return toResponse(commitment);
    }

    @Transactional
    public CommitmentResponse sendForReview(Long userId, Long commitmentId) {
        Commitment commitment = getCommitment(commitmentId);
        User actor = getUser(userId);

        if (commitment.getStatus() != CommitmentStatus.DRAFT) {
            throw new InvalidStateException("Can only send DRAFT for review");
        }

        commitment.setStatus(CommitmentStatus.REVIEW);
        commitment = commitmentRepository.save(commitment);
        emitEvent(commitment, actor, "SENT_FOR_REVIEW", null);
        return toResponse(commitment);
    }

    @Transactional
    public CommitmentResponse approve(Long userId, Long commitmentId) {
        Commitment commitment = getCommitment(commitmentId);
        User actor = getUser(userId);

        if (commitment.getStatus() != CommitmentStatus.REVIEW) {
            throw new InvalidStateException("Can only approve commitments in REVIEW");
        }

        CommitmentApprover approver = approverRepository.findByCommitmentIdAndUserId(commitmentId, userId)
            .orElseThrow(() -> new UnauthorizedException("User is not an approver"));

        if (approver.getStatus() != ApproverStatus.PENDING) {
            throw new InvalidStateException("Already acted on this commitment");
        }

        approver.setStatus(ApproverStatus.APPROVED);
        approver.setActedAt(OffsetDateTime.now());
        approverRepository.save(approver);
        emitEvent(commitment, actor, "APPROVED", null);

        boolean allApproved = !approverRepository.existsByCommitmentIdAndStatusNot(commitmentId, ApproverStatus.APPROVED);
        if (allApproved) {
            commitment.setStatus(CommitmentStatus.LOCKED);
            commitment = commitmentRepository.save(commitment);
            emitEvent(commitment, actor, "LOCKED", null);
        }

        return toResponse(commitment);
    }

    @Transactional
    public CommitmentResponse reject(Long userId, Long commitmentId) {
        Commitment commitment = getCommitment(commitmentId);
        User actor = getUser(userId);

        if (commitment.getStatus() != CommitmentStatus.REVIEW) {
            throw new InvalidStateException("Can only reject commitments in REVIEW");
        }

        CommitmentApprover approver = approverRepository.findByCommitmentIdAndUserId(commitmentId, userId)
            .orElseThrow(() -> new UnauthorizedException("User is not an approver"));

        if (approver.getStatus() != ApproverStatus.PENDING) {
            throw new InvalidStateException("Already acted on this commitment");
        }

        approver.setStatus(ApproverStatus.REJECTED);
        approver.setActedAt(OffsetDateTime.now());
        approverRepository.save(approver);

        commitment.setStatus(CommitmentStatus.DRAFT);
        commitment = commitmentRepository.save(commitment);

        // Reset all approvers to PENDING
        List<CommitmentApprover> allApprovers = approverRepository.findByCommitmentId(commitmentId);
        for (CommitmentApprover ca : allApprovers) {
            ca.setStatus(ApproverStatus.PENDING);
            ca.setActedAt(null);
            approverRepository.save(ca);
        }

        emitEvent(commitment, actor, "REJECTED", null);
        return toResponse(commitment);
    }

    public List<CommitmentResponse> getSpaceCommitments(Long userId, Long spaceId) {
        getUser(userId);
        spaceRepository.findById(spaceId)
            .orElseThrow(() -> new NotFoundException("Space not found"));
        if (!userSpaceRepository.existsByUserIdAndSpaceIdAndStatus(userId, spaceId, MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("User not member of space");
        }
        return commitmentRepository.findBySpaceId(spaceId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public CommitmentResponse getCommitmentById(Long userId, Long commitmentId) {
        Commitment commitment = getCommitment(commitmentId);
        if (!userSpaceRepository.existsByUserIdAndSpaceIdAndStatus(userId, commitment.getSpace().getId(), MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("User not member of space");
        }
        return toResponse(commitment);
    }

    public List<CommitmentHistoryResponse> getCommitmentHistory(Long userId, Long commitmentId) {
        Commitment commitment = getCommitment(commitmentId);
        if (!userSpaceRepository.existsByUserIdAndSpaceIdAndStatus(userId, commitment.getSpace().getId(), MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("User not member of space");
        }
        
        return eventRepository.findByCommitmentIdOrderByCreatedAtAsc(commitmentId).stream()
            .map(this::toHistoryResponse)
            .collect(Collectors.toList());
    }

    private Commitment getCommitment(Long id) {
        return commitmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Commitment not found"));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private OffsetDateTime toOffsetDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private void emitEvent(Commitment commitment, User actor, String eventType, String payload) {
        CommitmentEvent event = new CommitmentEvent();
        event.setCommitment(commitment);
        event.setActor(actor);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setCreatedAt(OffsetDateTime.now());
        eventRepository.save(event);
    }

    private CommitmentHistoryResponse toHistoryResponse(CommitmentEvent event) {
        return new CommitmentHistoryResponse(
            event.getId(),
            event.getEventType(),
            new PerformedByResponse(event.getActor().getId(), event.getActor().getName()),
            event.getCreatedAt(),
            event.getPayload()
        );
    }

    private CommitmentResponse toResponse(Commitment commitment) {
        List<ApproverResponse> approvers = approverRepository.findByCommitmentId(commitment.getId())
            .stream()
            .map(a -> new ApproverResponse(a.getUser().getId(), a.getUser().getName(), a.getStatus().name(), a.getActedAt()))
            .collect(Collectors.toList());

        return new CommitmentResponse(
            commitment.getId(),
            commitment.getSpace().getId(),
            commitment.getTitle(),
            commitment.getDescription(),
            commitment.getStatus().name(),
            commitment.getCreatedBy().getId(),
            commitment.getCreatedAt(),
            commitment.getDeadline(),
            approvers
        );
    }
}
