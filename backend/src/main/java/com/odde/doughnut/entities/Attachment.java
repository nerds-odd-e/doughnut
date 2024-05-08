package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@MappedSuperclass
public abstract class Attachment extends EntityIdentifiedByIdOnly {
  @NotNull
  @Size(min = 1, max = 255)
  @Getter
  @Setter
  private String name;

  @Getter @Setter private String type;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "attachment_blob_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private AttachmentBlob blob;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private User user;

  public ResponseEntity<byte[]> getResponseEntity(String disposition) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + getName() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, getType())
        .body(getBlob().getData());
  }
}
