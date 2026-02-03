package com.commit.commit.dto;

import java.time.OffsetDateTime;

public record SpaceResponse(
    Long id,
    String name,
    String description,
    Long createdBy,
    OffsetDateTime createdAt,
    int memberCount,
    int commitmentCount
) {}
