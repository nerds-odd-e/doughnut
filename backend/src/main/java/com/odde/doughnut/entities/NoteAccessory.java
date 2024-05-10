package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.controllers.dto.AudioUploadDTO;
import com.odde.doughnut.controllers.dto.NoteAccessoriesDTO;
import jakarta.persistence.*;
import java.io.IOException;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name = "note_accessory")
@JsonPropertyOrder({"audio", "audioName", "audioId"})
public class NoteAccessory extends EntityIdentifiedByIdOnly {

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Note note;

  @Getter @Setter private String url;

  @Column(name = "picture_url")
  @Getter
  @Setter
  private String pictureUrl;

  @Column(name = "picture_mask")
  @Getter
  @Setter
  private String pictureMask;

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "image_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Image imageAttachment;

  @Column(name = "use_parent_picture")
  @Getter
  @Setter
  private Boolean useParentPicture = false;

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "audio_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Audio audioAttachment;

  public Optional<Integer> getAudioId() {
    if (audioAttachment != null) {
      return Optional.of(audioAttachment.getId());
    }
    return null;
  }

  public Optional<String> getAudioName() {
    if (audioAttachment != null) {
      return Optional.of(audioAttachment.getName());
    }
    return null;
  }

  @JsonIgnore
  public void setAudio(AudioUploadDTO audioUploadDTO, User user) throws IOException {
    Audio audio = audioUploadDTO.fetchUploadedAudio();
    audio.setUser(user);
    setAudioAttachment(audio);
  }

  @JsonIgnore
  public void setFromDTO(NoteAccessoriesDTO noteAccessoriesDTO, User user) throws IOException {
    BeanUtils.copyProperties(noteAccessoriesDTO, this);
    Image uploadPicture = noteAccessoriesDTO.fetchUploadedPicture(user);
    if (uploadPicture != null) {
      setImageAttachment(uploadPicture);
    }
  }

  @JsonIgnore
  PictureWithMask getPictureWithMask() {
    String url = getNoteAccessoryContainingPicture().getUrlOfImage();
    if (url == null) return null;

    PictureWithMask pictureWithMask = new PictureWithMask();
    pictureWithMask.notePicture = url;
    pictureWithMask.pictureMask = pictureMask;
    return pictureWithMask;
  }

  private NoteAccessory getNoteAccessoryContainingPicture() {
    if (useParentPicture
        && note.getParent() != null
        && note.getParent().getNoteAccessory() != null) {
      return this.note.getParent().getNoteAccessory();
    }
    return this;
  }

  private String getUrlOfImage() {
    if (imageAttachment != null) {
      return "/attachments/images/" + imageAttachment.getId() + "/" + imageAttachment.getName();
    }
    if (!Strings.isBlank(pictureUrl)) {
      return pictureUrl;
    }
    return null;
  }
}
