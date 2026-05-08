package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.NoteAccessoriesDTO;
import jakarta.persistence.*;
import java.io.IOException;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;

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

  @JsonIgnore
  public void setFromDTO(NoteAccessoriesDTO noteAccessoriesDTO, User user) throws IOException {
    BeanUtils.copyProperties(noteAccessoriesDTO, this);
    Image uploadImage = noteAccessoriesDTO.fetchUploadedImage(user);
    if (uploadImage != null) {
      setImageAttachment(uploadImage);
    }
  }

  public ImageWithMask getImageWithMask() {
    String url = getUrlOfImage();
    if (url == null) return null;

    ImageWithMask imageWithMask = new ImageWithMask();
    imageWithMask.noteImage = url;
    imageWithMask.imageMask = imageMask;
    return imageWithMask;
  }

  private String getUrlOfImage() {
    if (imageAttachment != null) {
      return "/attachments/images/" + imageAttachment.getId() + "/" + imageAttachment.getName();
    }
    if (!Strings.isBlank(imageUrl)) {
      return imageUrl;
    }
    return null;
  }
}
