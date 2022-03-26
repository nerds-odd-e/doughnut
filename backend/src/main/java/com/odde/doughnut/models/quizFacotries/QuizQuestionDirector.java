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
    private final QuizQuestionServant servant;
    final ModelFactoryService modelFactoryService;
    private final QuizQuestionFactory quizQuestionFactory;

    public QuizQuestionDirector(QuizQuestion.QuestionType questionType, Randomizer randomizer, ReviewPoint reviewPoint, ModelFactoryService modelFactoryService) {
        this.questionType = questionType;
        this.randomizer = randomizer;
        this.reviewPoint = reviewPoint;
        this.modelFactoryService = modelFactoryService;
        this.servant = new QuizQuestionServant(randomizer, modelFactoryService);
        this.quizQuestionFactory = questionType.factory.apply(reviewPoint);
    }

    public QuizQuestion buildQuizQuestion() {
        if (!quizQuestionFactory.isValidQuestion()) {
            return null;
        }
        QuizQuestion quizQuestion = new QuizQuestion();
        quizQuestion.setReviewPoint(reviewPoint);
        quizQuestion.setQuestionType(questionType);
        List<Note> options = quizQuestionFactory.generateOptions(servant);
        if (quizQuestionFactory.minimumOptionCount() > options.size()) {
            return null;
        }
        quizQuestion.setOptionNotes(randomizer.shuffle(options));
        List<ReviewPoint> viceReviewPoints = quizQuestionFactory.getViceReviewPoints(modelFactoryService.toUserModel(reviewPoint.getUser()));

        if (quizQuestionFactory.minimumViceReviewPointCount() > viceReviewPoints.size()) {
            return null;
        }
        quizQuestion.setViceReviewPoints(viceReviewPoints);
        quizQuestion.setCategoryLink(quizQuestionFactory.getCategoryLink());
        return quizQuestion;
    }

}