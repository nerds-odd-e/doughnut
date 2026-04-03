package com.odde.doughnut.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "book_anchor")
public class BookAnchor extends EntityIdentifiedByIdOnly {

  @Column(name = "anchor_format", nullable = false)
  @Getter
  @Setter
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String anchorFormat;

  @Column(name = "value", nullable = false, columnDefinition = "TEXT")
  @Getter
  @Setter
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String value;
}
