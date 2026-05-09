package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "note_accessory")
public class NoteAccessory extends EntityIdentifiedByIdOnly {

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Note note;

  @Column(name = "image_url")
  @Getter
  @Setter
  private String imageUrl;

  @Column(name = "image_mask")
  @Getter
  @Setter
  private String imageMask;

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "image_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  private Image imageAttachment;

  public void setImageAttachment(Image image) {
    Image previous = this.imageAttachment;
    this.imageAttachment = image;
    if (previous != null && previous != image) {
      clearImageNoteIfOwnedByThisAccessory(previous);
    }
    if (image != null) {
      image.setNote(getNote());
    }
  }

  private void clearImageNoteIfOwnedByThisAccessory(Image previous) {
    Note accessoryNote = getNote();
    if (accessoryNote == null || previous.getNote() == null) return;
    if (Objects.equals(accessoryNote.getId(), previous.getNote().getId())) {
      previous.setNote(null);
    }
  }
}
