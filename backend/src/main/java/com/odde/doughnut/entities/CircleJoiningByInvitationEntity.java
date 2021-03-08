package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CircleJoiningByInvitationEntity {
    @NotNull
    @Size(min = 10, max = 20)
    @Getter
    @Setter
    String invitationCode;
}
