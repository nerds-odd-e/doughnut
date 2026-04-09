package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notebook_group")
public class NotebookGroup extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ownership_id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Ownership ownership;

  @Column(name = "name", nullable = false, length = 255)
  @Getter
  @Setter
  private String name;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  @Getter
  private Timestamp createdAt;
}
