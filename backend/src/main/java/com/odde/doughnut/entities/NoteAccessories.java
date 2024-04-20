package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

@Embeddable
@JsonPropertyOrder({"audio", "audioName", "audioId"})
public class NoteAccessories {

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
  private Image uploadPicture;

  @Column(name = "use_parent_picture")
  @Getter
  @Setter
  private Boolean useParentPicture = false;

  @JsonIgnore
  public Optional<String> getNotePicture() {
    if (uploadPicture != null) {
      return Optional.of(
          "/attachments/images/" + uploadPicture.getId() + "/" + uploadPicture.getName());
    }
    if (Strings.isBlank(pictureUrl)) return Optional.empty();
    return Optional.of(pictureUrl);
  }

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "audio_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Audio uploadAudio;

  public Optional<Integer> getAudioId() {
    if (uploadAudio != null) {
      return Optional.of(uploadAudio.getId());
    }
    return null;
  }

  public Optional<String> getAudioName() {
    if (uploadAudio != null) {
      return Optional.of(uploadAudio.getName());
    }
    return null;
  }
}
