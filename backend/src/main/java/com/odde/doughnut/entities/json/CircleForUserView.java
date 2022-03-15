package com.odde.doughnut.entities.json;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class CircleForUserView {
    @Setter
    @Getter
    Integer id;
    @Setter
    @Getter
    String name;
    @Setter
    @Getter
    String invitationCode;
    @Setter
    @Getter
    NotebooksViewedByUser notebooks;
    @Setter
    @Getter
    List<UserForOtherUserView> members;
}
