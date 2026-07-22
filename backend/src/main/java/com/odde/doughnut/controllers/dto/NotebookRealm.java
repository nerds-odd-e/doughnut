package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.odde.doughnut.entities.Notebook;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description =
        "Notebook chrome: entity plus optional catalog hints (attached book, readonly), and"
            + " optional container-owned readme markdown.")
public record NotebookRealm(
    @NotNull Notebook notebook,
    @JsonInclude(JsonInclude.Include.NON_NULL) Boolean hasAttachedBook,
    boolean readonly,
    @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(
            description =
                "Container-owned notebook readme markdown (populated by migration from legacy"
                    + " container note). Omitted when absent.")
        String readmeContent) {

  public static NotebookRealm of(
      Notebook notebook, Boolean hasAttachedBook, boolean readonly, String readmeContent) {
    return new NotebookRealm(notebook, hasAttachedBook, readonly, readmeContent);
  }
}
