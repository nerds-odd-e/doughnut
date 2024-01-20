package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "global_settings")
public class GlobalSettings {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

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
