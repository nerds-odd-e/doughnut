package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.factoryServices.ModelFactoryService;

import java.util.List;
import java.util.stream.Collectors;

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
        this.quizQuestionFactory = questionType.factory.apply(servant, reviewPoint);
    }

    public QuizQuestion buildQuizQuestion() {
        if (!quizQuestionFactory.isValidQuestion()) {
            return null;
        }
        QuizQuestion quizQuestion = new QuizQuestion(reviewPoint);
        quizQuestion.setQuestionType(questionType);
        quizQuestion.setOptions(generateOptions());
        quizQuestion.setDescription(quizQuestionFactory.generateInstruction());
        quizQuestion.setMainTopic(quizQuestionFactory.generateMainTopic());
        quizQuestion.setHintLinks(quizQuestionFactory.generateHintLinks());
        quizQuestion.setScope(getScope());
        quizQuestion.setViceReviewPointIds(getViceReviewPoinIds());
        return quizQuestion;
    }

    private List<Note> getScope() {
        List<Note> scope = quizQuestionFactory.generateScope();
        if (scope != null) return scope;
        return List.of(reviewPoint.getSourceNote().getNotebook().getHeadNote());
    }

    private List<Integer> getViceReviewPoinIds() {
        List<ReviewPoint> viceReviewPoints = quizQuestionFactory.getViceReviewPoints();
        if (viceReviewPoints == null) return null;
        return viceReviewPoints.stream().map(ReviewPoint::getId).collect(Collectors.toUnmodifiableList());
    }

    private List<QuizQuestion.Option> generateOptions() {
        List<Note> selectedList = quizQuestionFactory.generateFillingOptions(servant);
        selectedList.add(quizQuestionFactory.generateAnswerNote());
        randomizer.shuffle(selectedList);
        QuizQuestion.OptionCreator optionCreator = quizQuestionFactory.optionCreator();
        return selectedList.stream().map(optionCreator::optionFromNote).collect(Collectors.toUnmodifiableList());
    }
}