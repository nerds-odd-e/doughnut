package com.odde.doughnut.entities;

import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.entities.json.NoteSphere;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.quizFacotries.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class QuizQuestion {

    public enum QuestionType {
        CLOZE_SELECTION(ClozeTitleSelectionQuizFactory::new),
        SPELLING(SpellingQuizFactory::new),
        PICTURE_TITLE(PictureTitleSelectionQuizFactory::new),
        PICTURE_SELECTION(PictureSelectionQuizFactory::new),
        LINK_TARGET(LinkTargetQuizFactory::new),
        LINK_SOURCE(LinkSourceQuizFactory::new),
        CLOZE_LINK_TARGET(ClozeLinkTargetQuizFactory::new),
        DESCRIPTION_LINK_TARGET(DescriptionLinkTargetQuizFactory::new),
        WHICH_SPEC_HAS_INSTANCE(WhichSpecHasInstanceQuizFactory::new),
        FROM_SAME_PART_AS(FromSamePartAsQuizFactory::new),
        FROM_DIFFERENT_PART_AS(FromDifferentPartAsQuizFactory::new),
        LINK_SOURCE_EXCLUSIVE(LinkTargetExclusiveQuizFactory::new);

        public final Function<ReviewPoint, QuizQuestionFactory> factory;

        QuestionType(Function<ReviewPoint, QuizQuestionFactory> factory) {
            this.factory = factory;
        }
    }

    @Getter @Setter
    private QuestionType questionType;
    @Getter @Setter
    private List<Option> options;
    @Getter @Setter
    private String description;
    @Getter @Setter
    private String mainTopic;
    @Getter @Setter
    private Map<Link.LinkType, LinkViewed> hintLinks;
    @Getter @Setter
    private List<Integer> viceReviewPointIds;
    @Getter @Setter
    private List<Note> scope;


    public Answer buildAnswer() {
        Answer answer = new Answer();
        answer.setQuestionType(questionType);
        answer.setViceReviewPointIds(viceReviewPointIds);
        return answer;
    }

    public static class Option {
        @Getter private NoteSphere note;
        @Getter private boolean isPicture = false;

        private Option() { }

        public static Option createTitleOption(Note note) {
            Option option = new Option();
            option.note = new NoteViewer(null, note).toJsonObject();
            return option;
        }

        public static Option createPictureOption(Note note) {
            Option option = new Option();
            option.note = new NoteViewer(null, note).toJsonObject();
            option.isPicture = true;
            return option;
        }

        public String getDisplay() {
            return note.getNote().getTitle();
        }
    }

    public interface OptionCreator {
        Option optionFromNote(Note note);
    }

    public static class TitleOptionCreator implements OptionCreator {
        @Override
        public Option optionFromNote(Note note) {
            return Option.createTitleOption(note);
        }
    }

    public static class PictureOptionCreator implements OptionCreator {
        @Override
        public Option optionFromNote(Note note) {
            return Option.createPictureOption(note);
        }
    }
}
