package com.odde.doughnut.models;

import com.odde.doughnut.entities.AnswerEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.services.ModelFactoryService;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public List<Option> getOptions() {
        TreeNodeModel treeNodeModel = getAnswerTreeNodeModel();
        Stream<NoteEntity> noteEntityStream = treeNodeModel.getSiblings().stream()
                .filter(t -> !t.getTitle().equals(getAnswerNote().getTitle()));
        if(questionType == QuestionType.PICTURE_SELECTION) {
            noteEntityStream = noteEntityStream.filter(n -> Strings.isNotEmpty(n.getNotePicture()));
        }
        List<NoteEntity> list = noteEntityStream.collect(Collectors.toList());
        randomizer.shuffle(list);
        List<NoteEntity> selectedList = list.stream().limit(5).collect(Collectors.toList());
        selectedList.add(getAnswerNote());
        randomizer.shuffle(selectedList);

        if (questionType == QuestionType.PICTURE_SELECTION) {
            return toPictureOptions(selectedList);
        }

        return toTitleOptions(selectedList);
    }

    private List<Option> toPictureOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(Option::createPictureOption).collect(Collectors.toUnmodifiableList());
    }

    private List<Option> toTitleOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(Option::createTitleOption).collect(Collectors.toUnmodifiableList());
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

    private static class Option {
        @Getter private String value;
        @Getter private String display;
        @Getter private NoteEntity note;
        @Getter private boolean isPicture = false;

        private Option() {
        }

        static Option createTitleOption(NoteEntity note) {
            Option option = new Option();
            option.value = note.getTitle();
            option.display = option.value;
            return option;
        }

        static Option createPictureOption(NoteEntity note) {
            Option option = new Option();
            option.value = note.getTitle();
            option.note = note;
            option.isPicture = true;
            return option;
        }

    }
}
