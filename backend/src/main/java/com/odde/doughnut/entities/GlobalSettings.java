package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "global_settings")
public class GlobalSettings extends EntityIdentifiedByIdOnly {
  @NotNull
  @Column(name = "key_name")
  @Getter
  @Setter
  private String keyName;

  @NotNull
  @Column(name = "value")
  @Getter
  @Setter
  private String value;

  @NotNull
  @Column(name = "updated_at")
  @Getter
  @Setter
  private Timestamp updatedAt = new Timestamp(System.currentTimeMillis());
}
