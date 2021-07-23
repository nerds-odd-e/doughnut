package com.odde.doughnut.entities;

import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.quizFacotries.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class QuizQuestion {

    public enum QuestionType {
        CLOZE_SELECTION(ClozeTitleSelectionQuizFactory::new),
        SPELLING(SpellingQuizFactory::new),
        PICTURE_TITLE(PictureTitleSelectionQuizFactory::new),
        PICTURE_SELECTION(PictureSelectionQuizFactory::new),
        LINK_TARGET(LinkTargetQuizFactory::new),
        CLOZE_LINK_TARGET(ClozeLinkTargetQuizFactory::new),
        DESCRIPTION_LINK_TARGET(DescriptionLinkTargetQuizFactory::new),
        WHICH_SPEC_HAS_INSTANCE(WhichSpecHasInstanceQuizFactory::new),
        FROM_SAME_PART_AS(FromSamePartAsQuizFactory::new),
        FROM_DIFFERENT_PART_AS(FromDifferentPartAsQuizFactory::new),
        LINK_SOURCE_EXCLUSIVE(LinkTargetExclusiveQuizFactory::new);

        public final BiFunction<QuizQuestionServant, ReviewPoint, QuizQuestionFactory> factory;

        QuestionType(BiFunction<QuizQuestionServant, ReviewPoint, QuizQuestionFactory> factory) {
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
    @Getter @Setter
    private Map<Link.LinkType, LinkViewed> hintLinks;
    @Getter @Setter
    private List<Integer> viceReviewPointIds;


    public QuizQuestion(ReviewPoint reviewPoint) {
        this.reviewPoint = reviewPoint;
    }

    public boolean isPictureQuestion() {
        return questionType == QuestionType.PICTURE_TITLE;
    }

    public Answer buildAnswer() {
        Answer answer = new Answer();
        answer.setQuestionType(questionType);
        answer.setViceReviewPointIds(viceReviewPointIds);
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
