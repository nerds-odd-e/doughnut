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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * One non-Markdown file kept in a notebook's Portable tree: its complete filename, extension
 * included, accepted Git content, and optional folder placement. A null folder means the notebook
 * root. This row projects accepted Git content; Git remains the content authority. Filenames are
 * unique among sibling attachments under a binary collation, so distinct Git paths stay distinct
 * rows. Deleted along with its notebook; code removes it before removing its containing folder.
 *
 * <p>The {@code content} column holds accepted Git blob bytes only: a non-empty file's standard Git
 * LFS pointer, or nothing for an empty file. It never stores hydrated LFS object payloads. Digest
 * and size live in the pointer; this row does not duplicate them.
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
  private byte[] content;

  /**
   * Accepted Git blob bytes for this attachment: its standard Git LFS pointer, or empty for an
   * empty file. Never hydrated LFS object bytes.
   */
  public byte[] getAcceptedGitContent() {
    return content;
  }

  public void setAcceptedGitContent(byte[] acceptedGitContent) {
    this.content = acceptedGitContent;
  }

  /** JPA property for column {@code content}; prefer {@link #getAcceptedGitContent()}. */
  public byte[] getContent() {
    return getAcceptedGitContent();
  }

  /** JPA property for column {@code content}; prefer {@link #setAcceptedGitContent(byte[])}. */
  public void setContent(byte[] content) {
    setAcceptedGitContent(content);
  }

  public void requireInNotebook(Notebook notebook) {
    if (!getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not in notebook.");
    }
  }
}
