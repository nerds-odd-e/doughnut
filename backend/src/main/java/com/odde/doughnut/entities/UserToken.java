package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

  public UserToken(Integer userId, @NotNull String token, @NotNull String label) {
    this.userId = userId;
    this.token = token;
    this.label = label;
  }

  public UserToken() {}
}
