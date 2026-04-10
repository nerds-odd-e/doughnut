package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.odde.doughnut.services.book.BookBlockContentBboxItem;
import com.odde.doughnut.services.book.BookBlockContentBboxes;
import com.odde.doughnut.services.book.BookBlockDirectContentPredicate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "book_block")
@JsonPropertyOrder({
  "id",
  "startAnchor",
  "siblingOrder",
  "title",
  "parentBlockId",
  "hasDirectContent",
  "contentBboxes"
})
public class BookBlock extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Book book;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_block_id")
  @JsonIgnore
  @Getter
  @Setter
  private BookBlock parent;

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

  @JsonProperty("parentBlockId")
  @JsonView(BookViews.Full.class)
  @Schema(type = "integer")
  public Integer getParentBlockId() {
    return parent == null ? null : parent.getId();
  }

  @OneToMany(mappedBy = "bookBlock", fetch = FetchType.LAZY)
  @OrderBy("siblingOrder ASC")
  @JsonIgnore
  private final List<BookContentBlock> contentBlocks = new ArrayList<>();

  @JsonProperty("hasDirectContent")
  @JsonView(BookViews.Full.class)
  public boolean getHasDirectContent() {
    return BookBlockDirectContentPredicate.hasDirectContent(contentBlocks);
  }

  @JsonProperty("contentBboxes")
  @JsonView(BookViews.Full.class)
  public List<BookBlockContentBboxItem> getContentBboxes() {
    return BookBlockContentBboxes.fromOrderedBlocks(contentBlocks);
  }
}
