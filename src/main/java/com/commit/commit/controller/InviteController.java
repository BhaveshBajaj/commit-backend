package com.commit.commit.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commit.commit.dto.InviteResponse;
import com.commit.commit.security.AuthenticatedUser;
import com.commit.commit.service.SpaceService;

@RestController
@RequestMapping("/invites")
public class InviteController {
    private final SpaceService spaceService;

    public InviteController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping
    public List<InviteResponse> getPendingInvites() {
        return spaceService.getPendingInvites(AuthenticatedUser.getUserId());
    }

    @PostMapping("/{id}/accept")
    public void acceptInvite(@PathVariable Long id) {
        spaceService.acceptInvite(AuthenticatedUser.getUserId(), id);
    }

    @PostMapping("/{id}/reject")
    public void rejectInvite(@PathVariable Long id) {
        spaceService.rejectInvite(AuthenticatedUser.getUserId(), id);
    }
}
