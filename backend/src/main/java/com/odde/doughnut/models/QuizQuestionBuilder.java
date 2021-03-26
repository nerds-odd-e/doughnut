package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.odde.doughnut.models.QuizQuestion.QuestionType.*;

public class QuizQuestionBuilder {
    private final QuizQuestion.QuestionType questionType;
    private final Randomizer randomizer;
    private final ReviewPointEntity reviewPointEntity;
    final ModelFactoryService modelFactoryService;

    public QuizQuestionBuilder(QuizQuestion.QuestionType questionType, Randomizer randomizer, ReviewPointEntity reviewPointEntity, ModelFactoryService modelFactoryService) {
        this.questionType = questionType;
        this.randomizer = randomizer;
        this.reviewPointEntity = reviewPointEntity;
        this.modelFactoryService = modelFactoryService;
    }

    QuizQuestion buildQuizQuestion() {
        QuizQuestion quizQuestion = new QuizQuestion(reviewPointEntity, randomizer, modelFactoryService);
        quizQuestion.setQuestionType(questionType);
        quizQuestion.setOptions(generateOptions());
        quizQuestion.setDescription(buildDescription());
        return quizQuestion;
    }

    private List<QuizQuestion.Option> generateOptions() {
        TreeNodeModel treeNodeModel = getAnswerTreeNodeModel();
        Stream<NoteEntity> noteEntityStream = treeNodeModel.getSiblings().stream()
                .filter(t -> !t.getTitle().equals(getAnswerNote().getTitle()));
        if (questionType == PICTURE_SELECTION) {
            noteEntityStream = noteEntityStream.filter(n -> Strings.isNotEmpty(n.getNotePicture()));
        }
        List<NoteEntity> list = noteEntityStream.collect(Collectors.toList());
        randomizer.shuffle(list);
        List<NoteEntity> selectedList = list.stream().limit(5).collect(Collectors.toList());
        selectedList.add(getAnswerNote());
        randomizer.shuffle(selectedList);

        if (questionType == PICTURE_SELECTION) {
            return toPictureOptions(selectedList);
        }

        return toTitleOptions(selectedList);
    }

    private String buildDescription() {
        if (questionType.equals(PICTURE_SELECTION)) {
            return getAnswerNote().getTitle();
        }
        if (questionType.equals(LINK_TARGET)) {
            return reviewPointEntity.getLinkEntity().getQuizDescription();
        }
        if (questionType.equals(LINK_SOURCE_EXCLUSIVE)) {
            return String.format("Which of the following %s", reviewPointEntity.getExclusiveQuestion());
        }
        return getAnswerNote().getClozeDescription();
    }

    private List<QuizQuestion.Option> toPictureOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createPictureOption).collect(Collectors.toUnmodifiableList());
    }

    private List<QuizQuestion.Option> toTitleOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createTitleOption).collect(Collectors.toUnmodifiableList());
    }

    private TreeNodeModel getAnswerTreeNodeModel() {
        return modelFactoryService.toTreeNodeModel(getAnswerNote());
    }

    private NoteEntity getAnswerNote() {
        if (questionType == LINK_TARGET || questionType == LINK_SOURCE_EXCLUSIVE) {
            return reviewPointEntity.getLinkEntity().getTargetNote();
        }
        return reviewPointEntity.getNoteEntity();
    }

}