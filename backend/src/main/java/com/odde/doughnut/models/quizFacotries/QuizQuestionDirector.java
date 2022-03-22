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
        Note answerNote = quizQuestionFactory.generateAnswerNote(servant);
        if (answerNote == null) {
            return null;
        }
        if (!quizQuestionFactory.isValidQuestion()) {
            return null;
        }
        List<Note> fillingOptions = quizQuestionFactory.generateFillingOptions(servant);
        if(quizQuestionFactory.minimumFillingOptionCount() > 0 && fillingOptions.size() < quizQuestionFactory.minimumFillingOptionCount()) {
            return null;
        }
        List<ReviewPoint> viceReviewPoints = quizQuestionFactory.getViceReviewPoints(modelFactoryService.toUserModel(reviewPoint.getUser()));

        if(quizQuestionFactory.minimumViceReviewPointCount() > 0 && viceReviewPoints.size() < quizQuestionFactory.minimumViceReviewPointCount()) {
            return null;
        }
        QuizQuestion quizQuestion = new QuizQuestion(reviewPoint);
        quizQuestion.setQuestionType(questionType);
        quizQuestion.setViceReviewPoints( viceReviewPoints );
        List<Integer> viceReviewPoinIds = null;
        if (viceReviewPoints != null) {
            viceReviewPoinIds = viceReviewPoints.stream().map(ReviewPoint::getId).toList();
        }

        quizQuestion.setViceReviewPointIds(viceReviewPoinIds);
        quizQuestion.setCategoryLink(quizQuestionFactory.getCategoryLink());
        quizQuestion.setOptions(generateOptions(fillingOptions, answerNote));
        quizQuestion.setMainTopic(quizQuestionFactory.generateMainTopic());
        return quizQuestion;
    }

    private List<Note> getScope() {
        return List.of(reviewPoint.getSourceNote().getNotebook().getHeadNote());
    }

    private List<QuizQuestion.Option> generateOptions(List<Note> fillingOptions, Note answerNote) {
        fillingOptions.add(answerNote);
        randomizer.shuffle(fillingOptions);
        QuizQuestion.OptionCreator optionCreator = quizQuestionFactory.optionCreator();
        return fillingOptions.stream().map(optionCreator::optionFromNote).toList();
    }
}