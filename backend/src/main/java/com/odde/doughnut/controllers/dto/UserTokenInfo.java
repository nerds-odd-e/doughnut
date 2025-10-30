package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.UserToken;
import jakarta.validation.constraints.NotNull;

public class UserTokenInfo {
  @NotNull public UserToken userToken;

  public UserTokenInfo(UserToken userToken) {
    this.userToken = userToken;
  }
}
