package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.UserToken;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;

public class UserTokenInfo {
  @NotNull public UserToken userToken;
  @NotNull public String status;

  public UserTokenInfo(UserToken userToken, Timestamp now) {
    this.userToken = userToken;
    this.status = userToken.getExpiresAt().before(now) ? "Invalid" : "Valid";
  }
}
