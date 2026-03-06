package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;

public record GeneratedTokenDTO(
    @NotNull Integer id, @NotNull String token, @NotNull String label) {}
