package com.odde.donut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.sql.Timestamp;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

@NoArgsConstructor
@Data
public class NoteTopology {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @NonNull
  private Integer id;

  @NotBlank private String title;

  private Timestamp createdAt;

  private Timestamp updatedAt;

  public int getId() {
    return this.id;
  }
}
