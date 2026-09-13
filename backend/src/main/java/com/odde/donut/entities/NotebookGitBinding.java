package com.odde.donut.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

/**
 * The single accepted Git binding for one notebook: the notebook's current accepted head object ID
 * and the bundle bytes for that head. One row per notebook (enforced by a unique constraint on
 * {@code notebook_id}); deleted along with its notebook.
 */
@Entity
@Table(name = "notebook_git_binding")
public class NotebookGitBinding extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notebook_id", nullable = false)
  @Getter
  @Setter
  private Notebook notebook;

  @Column(name = "accepted_git_object_id", nullable = false, length = 40)
  @Getter
  @Setter
  private String acceptedGitObjectId;

  @Lob
  @Column(name = "bundle_bytes", nullable = false)
  @Getter
  @Setter
  private byte[] bundleBytes;

  @Column(name = "created_at", nullable = false)
  @Getter
  @Setter
  private Timestamp createdAt;

  @Column(name = "updated_at", nullable = false)
  @Getter
  @Setter
  private Timestamp updatedAt;
}
