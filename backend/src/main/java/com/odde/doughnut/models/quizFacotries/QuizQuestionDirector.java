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
        Note answerNote = quizQuestionFactory.generateAnswerNote(servant);
        if (answerNote == null) {
            return null;
        }
        if (!quizQuestionFactory.isValidQuestion()) {
            return null;
        }
        QuizQuestion quizQuestion = new QuizQuestion(reviewPoint);
        quizQuestion.setQuestionType(questionType);
        List<Note> fillingOptions = quizQuestionFactory.generateFillingOptions(servant);
        if(quizQuestionFactory.minimumFillingOptionCount() > 0 && fillingOptions.size() < quizQuestionFactory.minimumFillingOptionCount()) {
            return null;
        }
        List<Integer> viceReviewPoinIds = getViceReviewPoinIds();
        if(quizQuestionFactory.minimumViceReviewPointCount() > 0 && viceReviewPoinIds.size() < quizQuestionFactory.minimumViceReviewPointCount()) {
            return null;
        }
        quizQuestion.setViceReviewPointIds(viceReviewPoinIds);
        quizQuestion.setOptions(generateOptions(fillingOptions, answerNote));
        quizQuestion.setMainTopic(quizQuestionFactory.generateMainTopic());
        quizQuestion.setHintLinks(quizQuestionFactory.generateHintLinks());
        quizQuestion.setScope(getScope());
        quizQuestion.setDescription(quizQuestionFactory.generateInstruction());
        return quizQuestion;
    }

    private List<Note> getScope() {
        List<Note> scope = quizQuestionFactory.generateScope();
        if (scope != null) return scope;
        return List.of(reviewPoint.getSourceNote().getNotebook().getHeadNote());
    }

    private List<Integer> getViceReviewPoinIds() {
        List<ReviewPoint> viceReviewPoints = quizQuestionFactory.getViceReviewPoints(modelFactoryService.toUserModel(reviewPoint.getUser()));
        if (viceReviewPoints == null) return null;
        return viceReviewPoints.stream().map(ReviewPoint::getId).collect(Collectors.toUnmodifiableList());
    }

    private List<QuizQuestion.Option> generateOptions(List<Note> fillingOptions, Note answerNote) {
        fillingOptions.add(answerNote);
        randomizer.shuffle(fillingOptions);
        QuizQuestion.OptionCreator optionCreator = quizQuestionFactory.optionCreator();
        return fillingOptions.stream().map(optionCreator::optionFromNote).collect(Collectors.toUnmodifiableList());
    }
}