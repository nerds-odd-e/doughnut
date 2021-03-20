package com.odde.doughnut.models;

import com.odde.doughnut.entities.AnswerEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QuizQuestionGenerator {
    private final NoteEntity noteEntity;
    private final Randomizer randomizer;

    public QuizQuestionGenerator(ReviewPointEntity reviewPointEntity, Randomizer randomizer) {
        this.noteEntity = reviewPointEntity.getNoteEntity();
        this.randomizer = randomizer;
    }

    public QuizQuestion.QuestionType generateQuestionType() {
        ReviewSettingEntity reviewSettingEntity = noteEntity.getMasterReviewSettingEntity();
        List<QuizQuestion.QuestionType> questionTypes = new ArrayList<>();
        if(reviewSettingEntity != null && reviewSettingEntity.getRememberSpelling()) {
            questionTypes.add(QuizQuestion.QuestionType.SPELLING);
        }
        questionTypes.add(QuizQuestion.QuestionType.CLOZE_SELECTION);
        return randomizer.chooseOneRandomly(questionTypes);
    }
}
