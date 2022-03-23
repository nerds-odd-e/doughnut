package com.odde.doughnut.entities;

import com.odde.doughnut.entities.json.RepetitionForUser;

import java.util.Optional;

public class AnswerResult {
    public Integer answerId;
    public String answerDisplay;
    public boolean correct;
    public Optional<RepetitionForUser> nextRepetition;
}
