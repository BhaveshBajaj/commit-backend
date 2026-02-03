package com.commit.commit.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.commit.commit.dto.CreateSpaceRequest;
import com.commit.commit.dto.InviteToSpaceRequest;
import com.commit.commit.dto.MessageResponse;
import com.commit.commit.dto.SpaceMemberResponse;
import com.commit.commit.dto.SpaceResponse;
import com.commit.commit.dto.UserResponse;
import com.commit.commit.security.AuthenticatedUser;
import com.commit.commit.service.SpaceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/spaces")
public class SpaceController {
    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpaceResponse createSpace(@Valid @RequestBody CreateSpaceRequest request) {
        return spaceService.createSpace(AuthenticatedUser.getUserId(), request);
    }

    @PostMapping("/{spaceId}/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public void inviteToSpace(
            @PathVariable Long spaceId,
            @Valid @RequestBody InviteToSpaceRequest request) {
        spaceService.inviteToSpace(AuthenticatedUser.getUserId(), spaceId, request);
    }

    @GetMapping
    public List<SpaceResponse> getUserSpaces() {
        return spaceService.getUserSpaces(AuthenticatedUser.getUserId());
    }

    @GetMapping("/{spaceId}")
    public SpaceResponse getSpace(@PathVariable Long spaceId) {
        return spaceService.getSpace(spaceId);
    }

    @GetMapping("/{spaceId}/members")
    public List<SpaceMemberResponse> getSpaceMembers(@PathVariable Long spaceId) {
        return spaceService.getSpaceMembers(AuthenticatedUser.getUserId(), spaceId);
    }

    @GetMapping("/{spaceId}/members/search")
    public List<UserResponse> searchSpaceMembers(
            @PathVariable Long spaceId,
            @RequestParam(required = false) String q) {
        return spaceService.searchSpaceMembers(AuthenticatedUser.getUserId(), spaceId, q);
    }

    @PostMapping("/{spaceId}/leave")
    public MessageResponse leaveSpace(@PathVariable Long spaceId) {
        spaceService.leaveSpace(AuthenticatedUser.getUserId(), spaceId);
        return new MessageResponse("Successfully left the space");
    }
}
