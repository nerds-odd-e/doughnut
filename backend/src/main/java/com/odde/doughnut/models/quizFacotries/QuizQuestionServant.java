package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.models.QuizQuestion;

import java.util.List;
import java.util.stream.Collectors;

public class QuizQuestionServant {
    public QuizQuestionServant() {
    }

    List<QuizQuestion.Option> toPictureOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createPictureOption).collect(Collectors.toUnmodifiableList());
    }

    List<QuizQuestion.Option> toTitleOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createTitleOption).collect(Collectors.toUnmodifiableList());
    }
}