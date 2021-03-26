package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;

public class QuizQuestionGenerator {
    private final ReviewPointEntity reviewPointEntity;
    private final Randomizer randomizer;

    public QuizQuestionGenerator(ReviewPointEntity reviewPointEntity, Randomizer randomizer) {
        this.reviewPointEntity = reviewPointEntity;
        this.randomizer = randomizer;
    }

    public QuizQuestion.QuestionType generateQuestionType() {
        List<QuizQuestion.QuestionType> questionTypes = new ArrayList<>();
        if (reviewPointEntity.getLinkEntity() != null) {
            questionTypes.add(QuizQuestion.QuestionType.LINK_TARGET);
        }
        else {
            NoteEntity noteEntity = reviewPointEntity.getNoteEntity();
            if (!Strings.isEmpty(noteEntity.getDescription())) {
                ReviewSettingEntity reviewSettingEntity = noteEntity.getMasterReviewSettingEntity();
                if (reviewSettingEntity != null && reviewSettingEntity.getRememberSpelling()) {
                    questionTypes.add(QuizQuestion.QuestionType.SPELLING);
                }
                questionTypes.add(QuizQuestion.QuestionType.CLOZE_SELECTION);
            }
            if (!Strings.isEmpty(noteEntity.getNotePicture())) {
                questionTypes.add(QuizQuestion.QuestionType.PICTURE_TITLE);
                questionTypes.add(QuizQuestion.QuestionType.PICTURE_SELECTION);
            }
        }
        return randomizer.chooseOneRandomly(questionTypes);
    }

    QuizQuestion generateQuestion(Randomizer randomizer, ReviewPointEntity entity, ModelFactoryService modelFactoryService) {
        QuizQuestion quizQuestion = new QuizQuestion(entity, randomizer, modelFactoryService);
        QuizQuestion.QuestionType questionType = generateQuestionType();
        if (questionType == null) {
            return null;
        }
        quizQuestion.setQuestionType(questionType);

        return quizQuestion;
    }
}
