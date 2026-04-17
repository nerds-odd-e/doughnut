package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.book.BookBlockContentBboxes;
import com.odde.doughnut.services.book.BookReadingWireConstants;
import com.odde.doughnut.services.book.ContentLocator;
import com.odde.doughnut.services.book.PageBbox;
import com.odde.doughnut.services.book.PdfLocator;
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
@JsonPropertyOrder({
  "id",
  "depth",
  "title",
  "allBboxes",
  "contentLocators",
  "contentBlocks",
  "epubStartHref"
})
public class BookBlock extends EntityIdentifiedByIdOnly {

  private static final ObjectMapper MAPPER = new ObjectMapper();

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

  @JsonProperty("allBboxes")
  @JsonView(BookViews.Full.class)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public List<PageBbox> getAllBboxes() {
    return BookBlockContentBboxes.allBboxes(contentBlocks);
  }

  @JsonProperty("contentLocators")
  @JsonView(BookViews.Full.class)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public List<ContentLocator> getContentLocators() {
    if (book != null && BookReadingWireConstants.BOOK_FORMAT_EPUB.equals(book.getFormat())) {
      return List.of();
    }
    return BookBlockContentBboxes.allBboxes(contentBlocks).stream()
        .map(pb -> (ContentLocator) new PdfLocator(pb.pageIndex(), pb.bbox()))
        .toList();
  }

  @JsonProperty("contentBlocks")
  @JsonView(BookViews.Full.class)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public List<BookContentBlock> getContentBlocks() {
    return contentBlocks;
  }

  @JsonProperty("epubStartHref")
  @JsonView(BookViews.Full.class)
  @Schema(
      description =
          "EPUB-only block-start locator for layout navigation: spine XHTML path, optionally with"
              + " #fragment for a subsection anchor. Null for PDF or when unavailable.")
  public String getEpubStartHref() {
    if (book == null || !BookReadingWireConstants.BOOK_FORMAT_EPUB.equals(book.getFormat())) {
      return null;
    }
    if (contentBlocks == null || contentBlocks.isEmpty()) {
      return null;
    }
    String rawData = contentBlocks.getFirst().getRawData();
    if (rawData == null || rawData.isBlank()) {
      return null;
    }
    try {
      JsonNode n = MAPPER.readTree(rawData);
      if (n == null || !n.has("href") || !n.get("href").isTextual()) {
        return null;
      }
      String href = n.get("href").asText();
      if (href.isBlank()) {
        return null;
      }
      if (!n.has("fragment") || !n.get("fragment").isTextual()) {
        return href;
      }
      String fragment = n.get("fragment").asText();
      if (fragment.isBlank()) {
        return href;
      }
      return href + fragment;
    } catch (Exception e) {
      return null;
    }
  }
}
