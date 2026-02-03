package com.commit.commit.dto;

import java.time.OffsetDateTime;

public record ApproverResponse(
    Long userId,
    String name,
    String status,
    OffsetDateTime actedAt
) {}
