package com.commit.commit.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateCommitmentRequest(
    @NotBlank String title,
    String description,
    LocalDate deadline,
    @NotEmpty List<Long> approverIds
) {}
