package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.QuizQuestion;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.List;

public class QuizQuestionDirector {
    private final QuizQuestion.QuestionType questionType;
    private final Randomizer randomizer;
    private final ReviewPointEntity reviewPointEntity;
    private final NoteEntity answerNote;
    final ModelFactoryService modelFactoryService;
    private final QuizQuestionFactory linkTargetExclusiveQuizFactory;

    public QuizQuestionDirector(QuizQuestion.QuestionType questionType, Randomizer randomizer, ReviewPointEntity reviewPointEntity, ModelFactoryService modelFactoryService) {
        this.questionType = questionType;
        this.randomizer = randomizer;
        this.reviewPointEntity = reviewPointEntity;
        this.modelFactoryService = modelFactoryService;
        switch (questionType) {
            case LINK_SOURCE_EXCLUSIVE:
                this.linkTargetExclusiveQuizFactory = new LinkTargetExclusiveQuizFactory(reviewPointEntity, randomizer, modelFactoryService);
                break;
            case PICTURE_SELECTION:
                this.linkTargetExclusiveQuizFactory = new PictureSelectTitleQuizFactory(reviewPointEntity, randomizer, modelFactoryService);
                break;
            default:
                DefaultQuizFactory defaultQuizFactory = new DefaultQuizFactory(reviewPointEntity, randomizer, modelFactoryService);
                defaultQuizFactory.setQuestionType(questionType);
                this.linkTargetExclusiveQuizFactory = defaultQuizFactory;
                break;
        }
        this.answerNote = linkTargetExclusiveQuizFactory.generateAnswerNote();
    }

    public QuizQuestion buildQuizQuestion() {
        if (answerNote == null) {
            return null;
        }
        QuizQuestion quizQuestion = new QuizQuestion(reviewPointEntity, randomizer, modelFactoryService);
        quizQuestion.setQuestionType(questionType);
        quizQuestion.setOptions(generateOptions());
        quizQuestion.setDescription(linkTargetExclusiveQuizFactory.generateInstruction());
        quizQuestion.setMainTopic(linkTargetExclusiveQuizFactory.generateMainTopic());
        return quizQuestion;
    }

    private List<QuizQuestion.Option> generateOptions() {
        List<NoteEntity> selectedList = linkTargetExclusiveQuizFactory.generateFillingOptions();
        selectedList.add(answerNote);
        randomizer.shuffle(selectedList);
        return linkTargetExclusiveQuizFactory.toQuestionOptions(selectedList);
    }
}