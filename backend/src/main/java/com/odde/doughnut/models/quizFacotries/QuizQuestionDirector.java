package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.QuizQuestion;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.TreeNodeModel;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.odde.doughnut.models.QuizQuestion.QuestionType.*;

public class QuizQuestionDirector {
    private final QuizQuestion.QuestionType questionType;
    private final Randomizer randomizer;
    private final ReviewPointEntity reviewPointEntity;
    private final NoteEntity answerNote;
    final ModelFactoryService modelFactoryService;
    private final LinkTargetExclusiveQuizFactory linkTargetExclusiveQuizFactory;

    public QuizQuestionDirector(QuizQuestion.QuestionType questionType, Randomizer randomizer, ReviewPointEntity reviewPointEntity, ModelFactoryService modelFactoryService) {
        this.questionType = questionType;
        this.randomizer = randomizer;
        this.reviewPointEntity = reviewPointEntity;
        this.modelFactoryService = modelFactoryService;
        this.linkTargetExclusiveQuizFactory = new LinkTargetExclusiveQuizFactory(reviewPointEntity, randomizer, modelFactoryService);
        this.answerNote = getAnswerNote();
    }

    public QuizQuestion buildQuizQuestion() {
        if (answerNote == null) {
            return null;
        }
        QuizQuestion quizQuestion = new QuizQuestion(reviewPointEntity, randomizer, modelFactoryService);
        quizQuestion.setQuestionType(questionType);
        quizQuestion.setOptions(generateOptions());
        quizQuestion.setDescription(generateDescription());
        quizQuestion.setMainTopic(generateMainTopic());
        return quizQuestion;
    }

    private List<QuizQuestion.Option> generateOptions() {
        List<NoteEntity> selectedList;
        if (questionType == LINK_SOURCE_EXCLUSIVE) {
            selectedList = linkTargetExclusiveQuizFactory.generateFillingOptions();
        } else {
            Stream<NoteEntity> noteEntityStream;
            noteEntityStream = getAnswerTreeNodeModel().getSiblings().stream()
                    .filter(n -> !n.equals(answerNote));
            if (questionType == PICTURE_SELECTION) {
                noteEntityStream = noteEntityStream.filter(NoteEntity::hasPicture);
            }
            List<NoteEntity> list = noteEntityStream.collect(Collectors.toList());
            selectedList = randomizer.randomlyChoose(5, list);
        }
        selectedList.add(answerNote);
        randomizer.shuffle(selectedList);

        if (questionType == PICTURE_SELECTION) {
            return toPictureOptions(selectedList);
        }

        return toTitleOptions(selectedList);
    }

    private List<NoteEntity> generateFillingOptions() {
        return linkTargetExclusiveQuizFactory.generateFillingOptions();
    }

    private String generateDescription() {
        if (questionType.equals(PICTURE_SELECTION)) {
            return "";
        }
        if (questionType.equals(LINK_TARGET)) {
            return reviewPointEntity.getLinkEntity().getQuizDescription();
        }
        if (questionType.equals(LINK_SOURCE_EXCLUSIVE)) {
            return linkTargetExclusiveQuizFactory.generateInstruction();
        }
        return answerNote.getClozeDescription();
    }

    private String generateInstruction() {
        return linkTargetExclusiveQuizFactory.generateInstruction();
    }

    private String generateMainTopic() {
        if (questionType.equals(PICTURE_SELECTION)) {
            return answerNote.getTitle();
        }
        if (questionType.equals(LINK_SOURCE_EXCLUSIVE)) {
            return linkTargetExclusiveQuizFactory.generateMainTopic();
        }
        return "";
    }

    private String generateMainTopic1() {
        return linkTargetExclusiveQuizFactory.generateMainTopic();
    }

    private List<QuizQuestion.Option> toPictureOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createPictureOption).collect(Collectors.toUnmodifiableList());
    }

    private List<QuizQuestion.Option> toTitleOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createTitleOption).collect(Collectors.toUnmodifiableList());
    }

    private TreeNodeModel getAnswerTreeNodeModel() {
        return modelFactoryService.toTreeNodeModel(answerNote);
    }

    private NoteEntity getAnswerNote() {
        if (questionType == LINK_TARGET) {
            return reviewPointEntity.getLinkEntity().getTargetNote();
        }
        if (questionType == LINK_SOURCE_EXCLUSIVE) {
            return linkTargetExclusiveQuizFactory.generateAnswerNote();
        }
        return reviewPointEntity.getNoteEntity();
    }

}