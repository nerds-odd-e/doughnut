package com.odde.doughnut.entities;

import com.odde.doughnut.models.quizFacotries.*;
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
        public final BiFunction<QuizQuestionServant, ReviewPoint, QuizQuestionFactory> factory;

        QuestionType(String label, BiFunction<QuizQuestionServant, ReviewPoint, QuizQuestionFactory> factory) {
            this.label = label;
            this.factory = factory;
        }
    }

    private final ReviewPoint reviewPoint;
    @Getter @Setter
    private QuestionType questionType = null;
    @Getter @Setter
    public List<Option> options;
    @Getter @Setter
    public String description;
    @Getter @Setter
    public String mainTopic;

    public QuizQuestion(ReviewPoint reviewPoint) {
        this.reviewPoint = reviewPoint;
    }

    public boolean isPictureQuestion() {
        return questionType == QuestionType.PICTURE_TITLE;
    }

    public Answer buildAnswer() {
        Answer answer = new Answer();
        answer.setQuestionType(questionType);
        return answer;
    }

    public static class Option {
        @Getter private Note note;
        @Getter private boolean isPicture = false;

        private Option() { }

        public static Option createTitleOption(Note note) {
            Option option = new Option();
            option.note = note;
            return option;
        }

        public static Option createPictureOption(Note note) {
            Option option = new Option();
            option.note = note;
            option.isPicture = true;
            return option;
        }

        public String getDisplay() {
            return note.getTitle();
        }
    }
}
