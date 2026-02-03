package com.commit.commit.dto;

import java.time.OffsetDateTime;

public record CommitmentHistoryResponse(
    Long id,
    String action,
    PerformedByResponse performedBy,
    OffsetDateTime timestamp,
    String details
) {}
