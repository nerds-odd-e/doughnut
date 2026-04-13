package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "book_content_block")
@JsonPropertyOrder({"id", "type", "pageIdx", "raw"})
public class BookContentBlock extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_block_id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private BookBlock bookBlock;

  @Column(name = "sibling_order", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private int siblingOrder;

  @Column(name = "type", nullable = false, length = 128)
  @Getter
  @Setter
  @JsonView(BookViews.Full.class)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String type;

  @Column(name = "page_idx")
  @Getter
  @Setter
  @JsonView(BookViews.Full.class)
  private Integer pageIdx;

  @Column(name = "raw_data", nullable = false, columnDefinition = "LONGTEXT")
  @Getter
  @Setter
  @JsonProperty("raw")
  @JsonView(BookViews.Full.class)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String rawData;

  @Override
  @JsonView(BookViews.Full.class)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public Integer getId() {
    return super.getId();
  }
}
