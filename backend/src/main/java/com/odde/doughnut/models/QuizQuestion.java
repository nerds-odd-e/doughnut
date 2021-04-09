package com.odde.doughnut.models;

import com.odde.doughnut.entities.AnswerEntity;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.quizFacotries.*;
import com.odde.doughnut.services.ModelFactoryService;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.function.BiFunction;

public class QuizQuestion {

    public enum QuestionType {
        CLOZE_SELECTION("cloze_selection", DefaultQuizFactory::new),
        SPELLING("spelling", DefaultQuizFactory::new),
        PICTURE_TITLE("picture_title", DefaultQuizFactory::new),
        PICTURE_SELECTION("picture_selection", PictureSelectionQuizFactory::new),
        LINK_TARGET("link_target", LinkTargetQuizFactory::new),
        LINK_SOURCE_EXCLUSIVE("link_source_exclusive", LinkTargetExclusiveQuizFactory::new);

        public final String label;
        public final BiFunction<QuizQuestionServant, ReviewPointEntity, QuizQuestionFactory> factory;

        QuestionType(String label, BiFunction<QuizQuestionServant, ReviewPointEntity, QuizQuestionFactory> factory) {
            this.label = label;
            this.factory = factory;
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

    public static class Option {
        @Getter private String value;
        @Getter private String display;
        @Getter private Note note;
        @Getter private boolean isPicture = false;

        private Option() {
        }

        public static Option createTitleOption(Note note) {
            Option option = new Option();
            option.value = note.getTitle();
            option.display = option.value;
            return option;
        }

        public static Option createPictureOption(Note note) {
            Option option = new Option();
            option.value = note.getTitle();
            option.note = note;
            option.isPicture = true;
            return option;
        }

    }
}
