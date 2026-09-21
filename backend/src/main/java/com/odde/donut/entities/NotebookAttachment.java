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
 * One non-Markdown file kept in a notebook's Portable tree: its complete filename, extension
 * included, exact file bytes, and optional folder placement. A null folder means the notebook root.
 * This row projects accepted Git content; Git remains the content authority. Filenames are unique
 * among sibling attachments under a binary collation, so distinct Git paths stay distinct rows.
 * Deleted along with its notebook or containing folder.
 */
@Entity
@Table(name = "notebook_attachment")
public class NotebookAttachment extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notebook_id", nullable = false)
  @Getter
  @Setter
  private Notebook notebook;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "folder_id")
  @Getter
  @Setter
  private Folder folder;

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
