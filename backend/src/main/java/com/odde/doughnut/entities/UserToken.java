package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
  @JsonIgnore
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

  /**
   * Response-only token info for {@code GET /api/user/token-info} when the bearer value is a
   * non-persisted test token ({@link com.odde.doughnut.testability.TestAccessTokenResolver}). Not
   * saved; {@code id} is 0 so revoke-by-bearer does not delete a real row.
   */
  public static UserToken forTestabilityTokenInfo(User user) {
    UserToken t = new UserToken();
    t.id = 0;
    t.setUserId(user.getId());
    t.setLabel("Test access token (" + user.getExternalIdentifier() + ")");
    t.setToken("");
    return t;
  }
}
