package com.odde.donut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class NotebookSettings {
  /** JPQL fragment for note alias {@code n}: notebook is in the assimilation sequence. */
  public static final String JPA_NOTEBOOK_NOT_SKIP_MEMORY_TRACKING =
      "n.notebook.notebookSettings.skipMemoryTrackingEntirely = false";

  @Column(name = "skip_memory_tracking_entirely")
  Boolean skipMemoryTrackingEntirely = false;

  @JsonIgnore
  public void update(NotebookSettings value) {
    setSkipMemoryTrackingEntirely(value.getSkipMemoryTrackingEntirely());
  }
}
