package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_token")
public class UserToken extends EntityIdentifiedByIdOnly {

  @Column(name = "user_id")
  @Getter
  @Setter
  private Integer userId;

  @Column(name = "token")
  @Getter
  @Setter
  @NotNull
  private String token;

  @Column(name = "label")
  @Getter
  @Setter
  @NotNull
  private String label;

  @Column(name = "last_used_at")
  @Getter
  @Setter
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "JST")
  private Timestamp lastUsedAt;

  @Column(name = "expires_at")
  @Getter
  private Date expiresAt;

  public UserToken(Integer userId, @NotNull String token, @NotNull String label, Date expiresAt) {
    this.userId = userId;
    this.token = token;
    this.label = label;
    this.expiresAt = expiresAt;
  }

  public UserToken() {}
}
