package com.commit.commit.dto;

import java.time.OffsetDateTime;

public record SpaceMemberResponse(
    Long userId,
    String name,
    String email,
    String role,
    OffsetDateTime joinedAt
) {}
