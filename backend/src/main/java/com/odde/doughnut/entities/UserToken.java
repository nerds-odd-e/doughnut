package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
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
  private Timestamp lastUsedAt;

  @Column(name = "expires_at")
  private Date expiresAt;

  public UserToken(Integer userId, @NotNull String token, @NotNull String label) {
    this.userId = userId;
    this.token = token;
    this.label = label;

    LocalDate today = LocalDate.now();
    this.expiresAt = Date.valueOf(today.plusMonths(1));
  }

  public UserToken() {}

  @NotNull
  public String getStatus() {
    if (this.expiresAt == null) {
      return "Valid";
    }
    LocalDate today = LocalDate.now();
    if (this.expiresAt.before(Date.valueOf(today))) {
      return "Invalid";
    }
    return "Valid";
  }
}
