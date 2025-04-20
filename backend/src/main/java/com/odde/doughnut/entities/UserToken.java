package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_token")
@NoArgsConstructor
@Getter
@Setter
public class UserToken extends EntityIdentifiedByIdOnly {
  @NotNull
  @Column(unique = true)
  private String token;

  @ManyToOne
  @JoinColumn(name = "user_id")
  @NotNull
  private User user;

  @NotNull
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  public UserToken(User user, String token, LocalDateTime createdAt) {
    this.user = user;
    this.token = token;
    this.createdAt = createdAt;
  }
}
