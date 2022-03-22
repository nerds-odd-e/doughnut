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
        DESCRIPTION_LINK_TARGET(DescriptionLinkTargetQuizFactory::new, DescriptionLinkTargetQuizPresenter::new),
        WHICH_SPEC_HAS_INSTANCE(WhichSpecHasInstanceQuizFactory::new, WhichSpecHasInstanceQuizPresenter::new),
        FROM_SAME_PART_AS(FromSamePartAsQuizFactory::new, FromSamePartAsQuizPresenter::new),
        FROM_DIFFERENT_PART_AS(FromDifferentPartAsQuizFactory::new, FromDifferentPartAsQuizPresenter::new),
        LINK_SOURCE_EXCLUSIVE(LinkTargetExclusiveQuizFactory::new, LinkTargetExclusiveQuizPresenter::new);

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

    @Getter
    @Setter
    private QuestionType questionType;

    @Getter
    @Setter
    private List<Option> options;

    @JsonIgnore
    @Getter
    @Setter
    private Link categoryLink;

    private QuizQuestionPresenter getPresenter() {
        return this.questionType.presenter.apply(this);
    }

    public String getDescription() {
        return getPresenter().generateInstruction();
    }

    @Getter
    @Setter
    private String mainTopic;

    public Map<Link.LinkType, LinkViewed> getHintLinks() {
        return getPresenter().hintLinks();
    }

    @Getter
    @Setter
    private List<Integer> viceReviewPointIds;

    @JsonIgnore
    @Getter
    @Setter
    private List<ReviewPoint> viceReviewPoints;

    public List<Note> getScope() {
        return List.of(reviewPoint.getSourceNote().getNotebook().getHeadNote());
    }

    public Answer buildAnswer() {
        Answer answer = new Answer();
        answer.setQuestionType(questionType);
        answer.setViceReviewPointIds(viceReviewPointIds);
        return answer;
    }

    public static class Option {
        @Getter
        private NoteSphere note;
        @Getter
        private boolean isPicture = false;

        private Option() {
        }

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
