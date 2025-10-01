package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenConfigDTO {
    @NotNull
    @Size(min = 0, max = 100)
    @Getter
    @Setter
    private String label;
}
