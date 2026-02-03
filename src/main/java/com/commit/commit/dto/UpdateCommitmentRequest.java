package com.commit.commit.dto;

import java.time.LocalDate;

public record UpdateCommitmentRequest(
    String title,
    String description,
    LocalDate deadline
) {}
