package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "book")
@JsonPropertyOrder({"id", "bookName", "format", "createdAt", "updatedAt", "blocks", "notebookId"})
public class Book extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notebook_id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Notebook notebook;

  @JsonProperty("notebookId")
  @Schema(type = "integer", requiredMode = Schema.RequiredMode.REQUIRED)
  public Integer getNotebookId() {
    return notebook == null ? null : notebook.getId();
  }

  @Column(name = "book_name", nullable = false, length = 512)
  @Getter
  @Setter
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String bookName;

  @Column(name = "format", nullable = false, length = 32)
  @Getter
  @Setter
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String format;

  @Column(name = "source_file_ref", nullable = false, length = 1024)
  @Setter
  private String sourceFileRef;

  @JsonIgnore
  public String getSourceFileRef() {
    return sourceFileRef;
  }

  @Column(name = "created_at", nullable = false)
  @Getter
  @Setter
  private Timestamp createdAt;

  @Column(name = "updated_at", nullable = false)
  @Getter
  @Setter
  private Timestamp updatedAt;

  @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("layoutSequence ASC")
  private final List<BookBlock> blocks = new ArrayList<>();

  @JsonProperty("blocks")
  @JsonView(BookViews.Full.class)
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "Book blocks in depth-first preorder (parent before descendants, then siblings). "
              + "Order matches ascending layout_sequence in persistence.")
  public List<BookBlock> getBlocks() {
    return blocks;
  }

  public void addBlock(BookBlock block) {
    blocks.add(block);
    block.setBook(this);
  }
}
