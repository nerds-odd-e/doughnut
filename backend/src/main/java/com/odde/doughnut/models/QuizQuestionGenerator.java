package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizQuestionGenerator {
    private final ReviewPointEntity reviewPointEntity;
    private final Randomizer randomizer;

    public QuizQuestionGenerator(ReviewPointEntity reviewPointEntity, Randomizer randomizer) {
        this.reviewPointEntity = reviewPointEntity;
        this.randomizer = randomizer;
    }

    List<QuizQuestion.QuestionType> availableQuestionTypes() {
        List<QuizQuestion.QuestionType> questionTypes = new ArrayList<>();
        if (reviewPointEntity.getLink() != null) {
            Collections.addAll(questionTypes, reviewPointEntity.getLink().getLinkType().getQuestionTypes());
        }
        else {
            Note note = reviewPointEntity.getNote();
            if (!Strings.isEmpty(note.getNoteContent().getDescription())) {
                ReviewSettingEntity reviewSettingEntity = note.getMasterReviewSettingEntity();
                if (reviewSettingEntity != null && reviewSettingEntity.getRememberSpelling()) {
                    questionTypes.add(QuizQuestion.QuestionType.SPELLING);
                }
                questionTypes.add(QuizQuestion.QuestionType.CLOZE_SELECTION);
            }
            if (!Strings.isEmpty(note.getNotePicture())) {
                questionTypes.add(QuizQuestion.QuestionType.PICTURE_TITLE);
                questionTypes.add(QuizQuestion.QuestionType.PICTURE_SELECTION);
            }
        }
        return questionTypes;
    }

    QuizQuestion generateQuestion(ModelFactoryService modelFactoryService) {
        List<QuizQuestion.QuestionType> questionTypes = availableQuestionTypes();
        randomizer.shuffle(questionTypes);
        for(QuizQuestion.QuestionType type: questionTypes) {
            QuizQuestionDirector quizQuestionDirector = new QuizQuestionDirector(type, randomizer, reviewPointEntity, modelFactoryService);
            QuizQuestion quizQuestion = quizQuestionDirector.buildQuizQuestion();
            if (quizQuestion != null) return quizQuestion;
        }
        return null;
    }

}
