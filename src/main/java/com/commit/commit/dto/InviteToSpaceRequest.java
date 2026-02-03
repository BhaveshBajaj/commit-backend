package com.commit.commit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteToSpaceRequest(
    @NotBlank @Email String email
) {}
