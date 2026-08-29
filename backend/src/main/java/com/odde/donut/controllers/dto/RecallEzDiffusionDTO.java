package com.odde.donut.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Internal diagnostic (plan {@code 008-probe-convergent-analyses}, slice 3) reporting the
 * EZ-diffusion decomposition of the current user's trailing three-local-morning MCQ recall trials.
 * See {@code RecallEzDiffusion} for the trial-selection/pooling and {@code EzDiffusion} for the
 * closed-form algebra. Not user-facing.
 */
@Data
@AllArgsConstructor
public class RecallEzDiffusionDTO {
  private Double driftRate;
  private Double boundarySeparation;
  private Double nondecisionTimeMs;
  private Integer trialCount;
  private Integer morningCount;
}
