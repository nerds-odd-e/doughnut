package com.odde.doughnut.controllers.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenDTO {
  private String token;
  private LocalDateTime createdAt;
  private LocalDateTime expiresAt;
}
