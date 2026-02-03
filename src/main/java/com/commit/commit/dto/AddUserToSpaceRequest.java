package com.commit.commit.dto;

import jakarta.validation.constraints.NotNull;

public record AddUserToSpaceRequest(
    @NotNull Long userId
) {}
