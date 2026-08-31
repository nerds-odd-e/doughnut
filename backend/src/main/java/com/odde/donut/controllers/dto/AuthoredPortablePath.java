package com.odde.donut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthoredPortablePath(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String portablePath) {}
