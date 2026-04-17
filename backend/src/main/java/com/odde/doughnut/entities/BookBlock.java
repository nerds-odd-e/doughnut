package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.odde.doughnut.services.book.BookBlockContentLocatorAssembler;
import com.odde.doughnut.services.book.BookFormat;
import com.odde.doughnut.services.book.ContentLocator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "book_block")
@JsonPropertyOrder({"id", "depth", "title", "contentLocators", "contentBlocks"})
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

  @Column(
      name = "structural_title",
      nullable = false,
      length = BookBlockTitleLimits.STRUCTURAL_MAX_CHARS)
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
  @Fetch(FetchMode.SUBSELECT)
  private List<BookContentBlock> contentBlocks = new ArrayList<>();

  @JsonProperty("contentLocators")
  @JsonView(BookViews.Full.class)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public List<ContentLocator> getContentLocators() {
    return BookBlockContentLocatorAssembler.assemble(
        BookFormat.fromString(book != null ? book.getFormat() : null), contentBlocks);
  }

  @JsonProperty("contentBlocks")
  @JsonView(BookViews.Full.class)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public List<BookContentBlock> getContentBlocks() {
    return contentBlocks;
  }
}
