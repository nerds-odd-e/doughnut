package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class Book extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notebook_id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Notebook notebook;

  @JsonProperty("notebookId")
  @Schema(type = "integer")
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

  @Column(name = "source_file_ref", length = 1024)
  @Getter
  @Setter
  private String sourceFileRef;

  @Column(name = "created_at", nullable = false)
  @Getter
  @Setter
  private Timestamp createdAt;

  @Column(name = "updated_at", nullable = false)
  @Getter
  @Setter
  private Timestamp updatedAt;

  @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<BookRange> ranges = new ArrayList<>();

  @JsonView(BookViews.Full.class)
  public List<BookRange> getRanges() {
    return ranges;
  }

  public void addRange(BookRange range) {
    ranges.add(range);
    range.setBook(this);
  }
}
