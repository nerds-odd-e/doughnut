package com.odde.doughnut.entities.json;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CircleJoiningByInvitation {
    @NotNull
    @Size(min = 10, max = 20)
    @Getter
    @Setter
    String invitationCode;
}
