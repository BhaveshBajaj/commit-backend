package com.commit.commit.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CommitmentResponse(
    Long id,
    Long spaceId,
    String title,
    String description,
    String status,
    Long createdBy,
    OffsetDateTime createdAt,
    OffsetDateTime deadline,
    List<ApproverResponse> approvers
) {}
