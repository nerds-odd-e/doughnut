package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.User;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

public class UserForOtherUserView {
    @Setter
    @Getter
    String name;

    public static List<UserForOtherUserView> fromList(List<User> users) {
        return users.stream().map(u -> {
            UserForOtherUserView ufv = new UserForOtherUserView();
            ufv.setName(u.getName());
            return ufv;
        }).collect(Collectors.toUnmodifiableList());
    }
}
