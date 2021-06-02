package com.odde.doughnut.models;

import com.odde.doughnut.entities.Answer;

public class AnswerModel {
    protected final Answer entity;

    public AnswerModel(Answer answer) {
        this.entity = answer;
    }

    public boolean isCorrect() {
        return entity.checkAnswer();
    }

    public String getAnswerDisplay() {
        if (entity.getAnswerNote() != null) {
            return entity.getAnswerNote().getTitle();
        }
        return entity.getAnswer();
    }
}
