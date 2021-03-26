package com.odde.doughnut.models;

import com.odde.doughnut.entities.AnswerEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.services.ModelFactoryService;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QuizQuestion {
    public enum QuestionType {
        CLOZE_SELECTION("cloze_selection"),
        SPELLING("spelling"),
        PICTURE_TITLE("picture_title"),
        PICTURE_SELECTION("picture_selection"),
        LINK_TARGET("link_target");

        public final String label;

        QuestionType(String label) {
            this.label = label;
        }
    }

    private final Randomizer randomizer;
    private final ModelFactoryService modelFactoryService;
    private final ReviewPointEntity reviewPointEntity;
    @Getter @Setter
    private QuestionType questionType = null;

    public QuizQuestion(ReviewPointEntity reviewPointEntity, Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.reviewPointEntity = reviewPointEntity;
        this.randomizer = randomizer;
        this.modelFactoryService = modelFactoryService;
    }

    public boolean isPictureQuestion() {
        return questionType == QuestionType.PICTURE_TITLE;
    }

    public String getDescription() {
        if (questionType.equals(QuestionType.PICTURE_SELECTION)) {
            return getAnswerNote().getTitle();
        }
        if (questionType.equals(QuestionType.LINK_TARGET)) {
            return reviewPointEntity.getLinkEntity().getQuizDescription();
        }
        return getAnswerNote().getClozeDescription();
    }

    public List<String> getOptions() {
        TreeNodeModel treeNodeModel = getAnswerTreeNodeModel();
        List<String> list = treeNodeModel.getSiblings().stream().map(NoteEntity::getTitle)
                .filter(t->!t.equals(getAnswerNote().getTitle()))
                .collect(Collectors.toList());
        randomizer.shuffle(list);
        List<String> selectedList = list.stream().limit(5).collect(Collectors.toList());
        selectedList.add(getAnswerNote().getTitle());
        Collections.shuffle(selectedList);

        return selectedList;

    }

    public AnswerEntity buildAnswer() {
        AnswerEntity answerEntity = new AnswerEntity();
        answerEntity.setReviewPointEntity(reviewPointEntity);
        answerEntity.setQuestionType(questionType);
        return answerEntity;
    }

    private TreeNodeModel getAnswerTreeNodeModel() {
        return modelFactoryService.toTreeNodeModel(getAnswerNote());
    }

    private NoteEntity getAnswerNote() {
        if (questionType == QuestionType.LINK_TARGET) {
            return reviewPointEntity.getLinkEntity().getTargetNote();
        }
        return reviewPointEntity.getNoteEntity();
    }
}
