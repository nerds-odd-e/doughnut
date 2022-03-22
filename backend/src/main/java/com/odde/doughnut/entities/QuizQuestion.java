package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    public QuizQuestion(ReviewPoint reviewPoint) {

        this.reviewPoint = reviewPoint;
    }

    public enum QuestionType {
        CLOZE_SELECTION(ClozeTitleSelectionQuizFactory::new, ClozeTitleSelectionQuizPresenter::new),
        SPELLING(SpellingQuizFactory::new, SpellingQuizPresenter::new),
        PICTURE_TITLE(PictureTitleSelectionQuizFactory::new, PictureTitleSelectionQuizPresenter::new),
        PICTURE_SELECTION(PictureSelectionQuizFactory::new, PictureSelectionQuizPresenter::new),
        LINK_TARGET(LinkTargetQuizFactory::new, LinkTargetQuizPresenter::new),
        LINK_SOURCE(LinkSourceQuizFactory::new, LinkSourceQuizPresenter::new),
        CLOZE_LINK_TARGET(ClozeLinkTargetQuizFactory::new, ClozeLinkTargetQuizPresenter::new),
        DESCRIPTION_LINK_TARGET(DescriptionLinkTargetQuizFactory::new, null),
        WHICH_SPEC_HAS_INSTANCE(WhichSpecHasInstanceQuizFactory::new, null),
        FROM_SAME_PART_AS(FromSamePartAsQuizFactory::new, null),
        FROM_DIFFERENT_PART_AS(FromDifferentPartAsQuizFactory::new, null),
        LINK_SOURCE_EXCLUSIVE(LinkTargetExclusiveQuizFactory::new, null);

        public final Function<ReviewPoint, QuizQuestionFactory> factory;
        public final Function<QuizQuestion, QuizQuestionPresenter> presenter;

        QuestionType(Function<ReviewPoint, QuizQuestionFactory> factory, Function<QuizQuestion, QuizQuestionPresenter> presenter) {
            this.factory = factory;
            this.presenter = presenter;
        }
    }

    @JsonIgnore
    @Getter
    private ReviewPoint reviewPoint;

    @Getter @Setter
    private QuestionType questionType;
    @Getter @Setter
    private List<Option> options;

    private String description;

    private QuizQuestionPresenter getPresenter() {
        if(this.questionType.presenter != null)
            return this.questionType.presenter.apply(this);
        return null;
    }

    public String getDescription() {
        QuizQuestionPresenter presenter = getPresenter();
        if(presenter != null) return presenter.generateInstruction();
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

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
