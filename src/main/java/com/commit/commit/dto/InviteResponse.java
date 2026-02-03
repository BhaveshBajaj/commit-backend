package com.commit.commit.dto;

import java.time.OffsetDateTime;

public record InviteResponse(
    Long id,
    Long spaceId,
    String spaceName,
    String status,
    OffsetDateTime invitedAt
) {}
