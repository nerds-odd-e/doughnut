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
@Table(name = "book_range")
@JsonPropertyOrder({"id", "startAnchor", "siblingOrder", "title", "parentRangeId"})
public class BookRange extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Book book;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_range_id")
  @JsonIgnore
  @Getter
  @Setter
  private BookRange parent;

  @Column(name = "structural_title", nullable = false, length = 512)
  @Getter
  @Setter
  @JsonProperty("title")
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String structuralTitle;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "start_anchor_id", nullable = false)
  @Getter
  @Setter
  @JsonView(BookViews.Full.class)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private BookAnchor startAnchor;

  @Column(name = "sibling_order", nullable = false)
  @Getter
  @Setter
  @JsonView(BookViews.Full.class)
  private long siblingOrder;

  @JsonProperty("parentRangeId")
  @JsonView(BookViews.Full.class)
  @Schema(type = "integer")
  public Integer getParentRangeId() {
    return parent == null ? null : parent.getId();
  }
}
