package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizQuestionGenerator {
    private final ReviewPoint reviewPoint;
    private final Randomizer randomizer;

    public QuizQuestionGenerator(ReviewPoint reviewPoint, Randomizer randomizer) {
        this.reviewPoint = reviewPoint;
        this.randomizer = randomizer;
    }

    List<QuizQuestion.QuestionType> availableQuestionTypes() {
        List<QuizQuestion.QuestionType> questionTypes = new ArrayList<>();
        if (reviewPoint.getLink() != null) {
            Collections.addAll(questionTypes, reviewPoint.getLink().getLinkType().getQuestionTypes());
        }
        else {
            questionTypes.add(QuizQuestion.QuestionType.SPELLING);
            Note note = reviewPoint.getNote();
            if (!Strings.isEmpty(note.getNoteContent().getDescription())) {
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
            QuizQuestionDirector quizQuestionDirector = new QuizQuestionDirector(type, randomizer, reviewPoint, modelFactoryService);
            QuizQuestion quizQuestion = quizQuestionDirector.buildQuizQuestion();
            if (quizQuestion != null) return quizQuestion;
        }
        return null;
    }

}
