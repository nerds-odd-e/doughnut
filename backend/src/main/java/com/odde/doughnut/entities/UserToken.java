package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

  @Column(name = "expires_at")
  @Getter
  @Setter
  @NotNull
  private Timestamp expirationDate;

  @Getter
  @Setter
  @Transient
  private Boolean isExpired;

  public UserToken(Integer userId, @NotNull String token, @NotNull String label, Timestamp expirationDate) {
    this.userId = userId;
    this.token = token;
    this.label = label;
    this.expirationDate = expirationDate;
  }

  public UserToken() {}
}
