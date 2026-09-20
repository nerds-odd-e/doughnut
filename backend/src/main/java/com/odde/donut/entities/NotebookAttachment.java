package com.odde.donut.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * One non-Markdown file kept at a notebook's Portable root: its complete filename, extension
 * included, and its exact file bytes. This row projects accepted Git content; Git remains the
 * content authority. Filenames are unique per notebook under a binary collation, so distinct Git
 * paths stay distinct rows. Deleted along with its notebook.
 */
@Entity
@Table(name = "notebook_attachment")
public class NotebookAttachment extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notebook_id", nullable = false)
  @Getter
  @Setter
  private Notebook notebook;

  @Column(name = "filename", nullable = false)
  @Getter
  @Setter
  private String filename;

  @Lob
  @Column(name = "content", nullable = false)
  @Getter
  @Setter
  private byte[] content;
}
