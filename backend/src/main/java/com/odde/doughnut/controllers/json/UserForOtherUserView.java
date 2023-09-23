package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.User;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

public class UserForOtherUserView {
  @Setter @Getter String name;

  public static List<UserForOtherUserView> fromList(List<User> users) {
    return users.stream()
        .map(
            u -> {
              UserForOtherUserView ufv = new UserForOtherUserView();
              ufv.setName(u.getName());
              return ufv;
            })
        .collect(Collectors.toUnmodifiableList());
  }
}
