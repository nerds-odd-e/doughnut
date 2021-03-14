package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AnswerEntity {
    @NotNull
    @Getter
    @Setter
    String answer;
    @Getter
    @Setter
    ReviewPointEntity reviewPointEntity;
}
