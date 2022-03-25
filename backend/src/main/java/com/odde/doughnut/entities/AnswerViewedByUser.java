package com.odde.doughnut.entities;

import com.odde.doughnut.entities.json.ReviewPointViewedByUser;

import java.util.Optional;

public class AnswerViewedByUser {
    public Integer answerId;
    public String answerDisplay;
    public boolean correct;
    public ReviewPointViewedByUser reviewPoint;
}
