package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.odde.doughnut.services.book.BookBlockContentBboxes;
import com.odde.doughnut.services.book.PageBbox;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "book_block")
@JsonPropertyOrder({"id", "depth", "title", "allBboxes"})
public class BookBlock extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Book book;

  @Column(name = "layout_sequence", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private int layoutSequence;

  @Column(name = "depth", nullable = false)
  @Getter
  @Setter
  @JsonProperty("depth")
  @JsonView(BookViews.Full.class)
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Nesting depth in the book layout; root-level blocks are 0.")
  private int depth;

  @Column(name = "structural_title", nullable = false, length = 512)
  @Getter
  @Setter
  @JsonProperty("title")
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String structuralTitle;

  @OneToMany(
      mappedBy = "bookBlock",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @OrderBy("siblingOrder ASC")
  @JsonIgnore
  private final List<BookContentBlock> contentBlocks = new ArrayList<>();

  @JsonProperty("allBboxes")
  @JsonView(BookViews.Full.class)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public List<PageBbox> getAllBboxes() {
    return BookBlockContentBboxes.allBboxes(contentBlocks);
  }
}
