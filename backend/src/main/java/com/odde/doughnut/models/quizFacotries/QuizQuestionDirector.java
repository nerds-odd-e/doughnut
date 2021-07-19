package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.factoryServices.ModelFactoryService;

import java.util.List;

public class QuizQuestionDirector {
    private final QuizQuestion.QuestionType questionType;
    private final Randomizer randomizer;
    private final ReviewPoint reviewPoint;
    private final Note answerNote;
    final ModelFactoryService modelFactoryService;
    private final QuizQuestionFactory quizQuestionFactory;

    public QuizQuestionDirector(QuizQuestion.QuestionType questionType, Randomizer randomizer, ReviewPoint reviewPoint, ModelFactoryService modelFactoryService) {
        this.questionType = questionType;
        this.randomizer = randomizer;
        this.reviewPoint = reviewPoint;
        this.modelFactoryService = modelFactoryService;
        QuizQuestionServant servant = new QuizQuestionServant(randomizer, modelFactoryService);
        this.quizQuestionFactory = questionType.factory.apply(servant, reviewPoint);
        this.answerNote = quizQuestionFactory.generateAnswerNote();
    }

    public QuizQuestion buildQuizQuestion() {
        if (answerNote == null) {
            return null;
        }
        if (!quizQuestionFactory.isValidQuestion()) {
            return null;
        }
        QuizQuestion quizQuestion = new QuizQuestion(reviewPoint);
        quizQuestion.setQuestionType(questionType);
        quizQuestion.setOptions(generateOptions());
        quizQuestion.setDescription(quizQuestionFactory.generateInstruction());
        quizQuestion.setMainTopic(quizQuestionFactory.generateMainTopic());
        quizQuestion.setHintLinks(quizQuestionFactory.generateHintLinks());
        quizQuestion.setViceReviewPointId(getViceReviewPoinId());
        return quizQuestion;
    }

    private Integer getViceReviewPoinId() {
        ReviewPoint viceReviewPoint = quizQuestionFactory.getViceReviewPoint();
        if (viceReviewPoint == null) return null;
        return viceReviewPoint.getId();
    }

    private List<QuizQuestion.Option> generateOptions() {
        List<Note> selectedList = quizQuestionFactory.generateFillingOptions();
        selectedList.add(answerNote);
        randomizer.shuffle(selectedList);
        return quizQuestionFactory.toQuestionOptions(selectedList);
    }
}