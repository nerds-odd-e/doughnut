package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audio")
public class Audio {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Size(min = 1, max = 255)
  @Getter
  @Setter
  private String name;

  @Getter @Setter private String type;

  @Column(name = "storage_type")
  @Getter
  @Setter
  private String storageType;

  @Column(name = "audio_blob_id", insertable = false, updatable = false)
  @Getter
  private Integer audioBlobId;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "audio_blob_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private AudioBlob audioBlob;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private User user;
}
