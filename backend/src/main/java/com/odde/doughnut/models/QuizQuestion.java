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
        LINK_TARGET("link_target"),
        LINK_SOURCE_EXCLUSIVE("link_source_exclusive");

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
    @Getter @Setter
    public List<Option> options;
    @Getter @Setter
    public String description;
    @Getter @Setter
    public String mainTopic;

    public QuizQuestion(ReviewPointEntity reviewPointEntity, Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.reviewPointEntity = reviewPointEntity;
        this.randomizer = randomizer;
        this.modelFactoryService = modelFactoryService;
    }

    public boolean isPictureQuestion() {
        return questionType == QuestionType.PICTURE_TITLE;
    }

    public AnswerEntity buildAnswer() {
        AnswerEntity answerEntity = new AnswerEntity();
        answerEntity.setReviewPointEntity(reviewPointEntity);
        answerEntity.setQuestionType(questionType);
        return answerEntity;
    }

    static class Option {
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
