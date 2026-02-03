package com.commit.commit.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commit.commit.dto.CreateSpaceRequest;
import com.commit.commit.dto.InviteResponse;
import com.commit.commit.dto.InviteToSpaceRequest;
import com.commit.commit.dto.SpaceMemberResponse;
import com.commit.commit.dto.SpaceResponse;
import com.commit.commit.dto.UserResponse;
import com.commit.commit.entity.CommitmentStatus;
import com.commit.commit.entity.MembershipStatus;
import com.commit.commit.entity.Space;
import com.commit.commit.entity.User;
import com.commit.commit.entity.UserSpace;
import com.commit.commit.exception.InvalidStateException;
import com.commit.commit.exception.NotFoundException;
import com.commit.commit.exception.UnauthorizedException;
import com.commit.commit.repository.CommitmentRepository;
import com.commit.commit.repository.SpaceRepository;
import com.commit.commit.repository.UserRepository;
import com.commit.commit.repository.UserSpaceRepository;

@Service
public class SpaceService {
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final UserSpaceRepository userSpaceRepository;
    private final CommitmentRepository commitmentRepository;

    public SpaceService(SpaceRepository spaceRepository, UserRepository userRepository, 
                        UserSpaceRepository userSpaceRepository, CommitmentRepository commitmentRepository) {
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
        this.userSpaceRepository = userSpaceRepository;
        this.commitmentRepository = commitmentRepository;
    }

    @Transactional
    public SpaceResponse createSpace(Long userId, CreateSpaceRequest request) {
        User user = getUser(userId);

        Space space = new Space();
        space.setName(request.name());
        space.setDescription(request.description());
        space.setCreatedBy(user);
        space.setCreatedAt(OffsetDateTime.now());
        space = spaceRepository.save(space);

        UserSpace userSpace = new UserSpace();
        userSpace.setUser(user);
        userSpace.setSpace(space);
        userSpace.setStatus(MembershipStatus.APPROVED);
        userSpace.setJoinedAt(OffsetDateTime.now());
        userSpaceRepository.save(userSpace);

        return toResponse(space);
    }

    @Transactional
    public void inviteToSpace(Long inviterId, Long spaceId, InviteToSpaceRequest request) {
        Space space = spaceRepository.findById(spaceId)
            .orElseThrow(() -> new NotFoundException("Space not found"));

        // Check inviter is approved member
        if (!userSpaceRepository.existsByUserIdAndSpaceIdAndStatus(inviterId, spaceId, MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("Only approved members can invite");
        }

        // Find user by email
        User invitee = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new NotFoundException("User not found. Ask them to join the platform first."));

        // Check existing membership
        Optional<UserSpace> existing = userSpaceRepository.findByUserIdAndSpaceId(invitee.getId(), spaceId);
        if (existing.isPresent()) {
            MembershipStatus status = existing.get().getStatus();
            if (status == MembershipStatus.APPROVED) {
                throw new InvalidStateException("User is already a member");
            }
            if (status == MembershipStatus.PENDING) {
                throw new InvalidStateException("Invite already pending");
            }
            // REJECTED: allow re-invite by updating status
            UserSpace membership = existing.get();
            membership.setStatus(MembershipStatus.PENDING);
            membership.setJoinedAt(OffsetDateTime.now());
            userSpaceRepository.save(membership);
            return;
        }

        // Create new invite
        UserSpace userSpace = new UserSpace();
        userSpace.setUser(invitee);
        userSpace.setSpace(space);
        userSpace.setStatus(MembershipStatus.PENDING);
        userSpace.setJoinedAt(OffsetDateTime.now());
        userSpaceRepository.save(userSpace);
    }

    public List<InviteResponse> getPendingInvites(Long userId) {
        getUser(userId);
        return userSpaceRepository.findByUserIdAndStatusWithSpaceOnly(userId, MembershipStatus.PENDING).stream()
            .map(this::toInviteResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void acceptInvite(Long userId, Long inviteId) {
        UserSpace invite = userSpaceRepository.findByIdAndUserId(inviteId, userId)
            .orElseThrow(() -> new NotFoundException("Invite not found"));

        if (invite.getStatus() != MembershipStatus.PENDING) {
            throw new InvalidStateException("Invite is not pending");
        }

        invite.setStatus(MembershipStatus.APPROVED);
        invite.setJoinedAt(OffsetDateTime.now());
        userSpaceRepository.save(invite);
    }

    @Transactional
    public void rejectInvite(Long userId, Long inviteId) {
        UserSpace invite = userSpaceRepository.findByIdAndUserId(inviteId, userId)
            .orElseThrow(() -> new NotFoundException("Invite not found"));

        if (invite.getStatus() != MembershipStatus.PENDING) {
            throw new InvalidStateException("Invite is not pending");
        }

        invite.setStatus(MembershipStatus.REJECTED);
        userSpaceRepository.save(invite);
    }

    public List<SpaceResponse> getUserSpaces(Long userId) {
        getUser(userId);
        return userSpaceRepository.findByUserIdAndStatusWithSpace(userId, MembershipStatus.APPROVED).stream()
            .map(us -> toResponse(us.getSpace()))
            .collect(Collectors.toList());
    }

    public SpaceResponse getSpace(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
            .orElseThrow(() -> new NotFoundException("Space not found"));
        return toResponse(space);
    }

    public boolean isApprovedMember(Long userId, Long spaceId) {
        return userSpaceRepository.existsByUserIdAndSpaceIdAndStatus(userId, spaceId, MembershipStatus.APPROVED);
    }

    public List<SpaceMemberResponse> getSpaceMembers(Long userId, Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
            .orElseThrow(() -> new NotFoundException("Space not found"));
        
        if (!userSpaceRepository.existsByUserIdAndSpaceIdAndStatus(userId, spaceId, MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("User not member of space");
        }
        
        return userSpaceRepository.findBySpaceIdAndStatusWithUser(spaceId, MembershipStatus.APPROVED).stream()
            .map(us -> toMemberResponse(us, space))
            .collect(Collectors.toList());
    }

    public List<UserResponse> searchSpaceMembers(Long userId, Long spaceId, String query) {
        spaceRepository.findById(spaceId)
            .orElseThrow(() -> new NotFoundException("Space not found"));
        
        if (!userSpaceRepository.existsByUserIdAndSpaceIdAndStatus(userId, spaceId, MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("User not member of space");
        }
        
        List<UserSpace> members;
        if (query == null || query.trim().isEmpty()) {
            members = userSpaceRepository.findBySpaceIdAndStatusWithUser(spaceId, MembershipStatus.APPROVED);
        } else {
            members = userSpaceRepository.searchMembersByQuery(spaceId, MembershipStatus.APPROVED, query.trim());
        }
        
        return members.stream()
            .map(us -> new UserResponse(us.getUser().getId(), us.getUser().getName(), us.getUser().getEmail()))
            .collect(Collectors.toList());
    }

    @Transactional
    public void leaveSpace(Long userId, Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
            .orElseThrow(() -> new NotFoundException("Space not found"));
        
        UserSpace membership = userSpaceRepository.findByUserIdAndSpaceId(userId, spaceId)
            .orElseThrow(() -> new NotFoundException("User not a member of this space"));
        
        if (membership.getStatus() != MembershipStatus.APPROVED) {
            throw new NotFoundException("User not a member of this space");
        }
        
        // Space creator cannot leave
        if (space.getCreatedBy().getId().equals(userId)) {
            throw new InvalidStateException("Space creator cannot leave the space");
        }
        
        // Check if user has commitments in REVIEW status
        if (commitmentRepository.existsBySpaceIdAndCreatedByIdAndStatus(spaceId, userId, CommitmentStatus.REVIEW)) {
            throw new InvalidStateException("Cannot leave space while you have commitments in review");
        }
        
        userSpaceRepository.delete(membership);
    }

    private SpaceMemberResponse toMemberResponse(UserSpace us, Space space) {
        String role = space.getCreatedBy().getId().equals(us.getUser().getId()) ? "creator" : "member";
        return new SpaceMemberResponse(
            us.getUser().getId(),
            us.getUser().getName(),
            us.getUser().getEmail(),
            role,
            us.getJoinedAt()
        );
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private SpaceResponse toResponse(Space space) {
        int memberCount = userSpaceRepository.countBySpaceIdAndStatus(space.getId(), MembershipStatus.APPROVED);
        int commitmentCount = commitmentRepository.countBySpaceId(space.getId());
        
        return new SpaceResponse(
            space.getId(),
            space.getName(),
            space.getDescription(),
            space.getCreatedBy().getId(),
            space.getCreatedAt(),
            memberCount,
            commitmentCount
        );
    }

    private InviteResponse toInviteResponse(UserSpace us) {
        return new InviteResponse(
            us.getId(),
            us.getSpace().getId(),
            us.getSpace().getName(),
            us.getStatus().name(),
            us.getJoinedAt()
        );
    }
}
