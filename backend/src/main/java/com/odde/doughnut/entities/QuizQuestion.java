package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.entities.json.NoteSphere;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.quizFacotries.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Table(name = "quiz_question")
public class QuizQuestion {

    public enum QuestionType {
        CLOZE_SELECTION(1, ClozeTitleSelectionQuizFactory::new, ClozeTitleSelectionQuizPresenter::new),
        SPELLING(2, SpellingQuizFactory::new, SpellingQuizPresenter::new),
        PICTURE_TITLE(3, PictureTitleSelectionQuizFactory::new, PictureTitleSelectionQuizPresenter::new),
        PICTURE_SELECTION(4, PictureSelectionQuizFactory::new, PictureSelectionQuizPresenter::new),
        LINK_TARGET(5, LinkTargetQuizFactory::new, LinkTargetQuizPresenter::new),
        LINK_SOURCE(6, LinkSourceQuizFactory::new, LinkSourceQuizPresenter::new),
        CLOZE_LINK_TARGET(7, ClozeLinkTargetQuizFactory::new, ClozeLinkTargetQuizPresenter::new),
        DESCRIPTION_LINK_TARGET(8, DescriptionLinkTargetQuizFactory::new, DescriptionLinkTargetQuizPresenter::new),
        WHICH_SPEC_HAS_INSTANCE(9, WhichSpecHasInstanceQuizFactory::new, WhichSpecHasInstanceQuizPresenter::new),
        FROM_SAME_PART_AS(10, FromSamePartAsQuizFactory::new, FromSamePartAsQuizPresenter::new),
        FROM_DIFFERENT_PART_AS(11, FromDifferentPartAsQuizFactory::new, FromDifferentPartAsQuizPresenter::new),
        LINK_SOURCE_EXCLUSIVE(12, LinkTargetExclusiveQuizFactory::new, LinkTargetExclusiveQuizPresenter::new);

        public final Integer id;
        public final Function<ReviewPoint, QuizQuestionFactory> factory;
        public final Function<QuizQuestion, QuizQuestionPresenter> presenter;

        QuestionType(Integer id, Function<ReviewPoint, QuizQuestionFactory> factory, Function<QuizQuestion, QuizQuestionPresenter> presenter) {
            this.id = id;
            this.factory = factory;
            this.presenter = presenter;
        }

        private static final Map<Integer, QuestionType> idMap = Collections.unmodifiableMap(Arrays.stream(values()).collect(Collectors.toMap(x->x.id, x->x)));

        public static QuestionType fromId(Integer id) {
            return idMap.getOrDefault(id, null);
        }
    }

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "review_point_id", referencedColumnName = "id")
    @Getter @Setter
    private ReviewPoint reviewPoint;

    @Column(name = "question_type")
    @Getter @Setter
    private Integer questionTypeId;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "category_link_id", referencedColumnName = "id")
    @Getter
    @Setter
    private Link categoryLink;

    @JsonIgnore
    @Column(name = "option_notes")
    @Getter @Setter
    private String optionNoteIds = "";

    @JsonIgnore
    @Column(name = "vice_review_point_ids")
    @Getter
    private String viceReviewPointIds = "";

    public void setViceReviewPoints(List<ReviewPoint> reviewPoints) {
        if(reviewPoints == null) {
            viceReviewPointIds = "";
            return;
        }
        viceReviewPointIds = reviewPoints.stream().map(ReviewPoint::getId).map(Object::toString).collect(Collectors.joining(","));
    }

    @JsonIgnore
    @Transient
    private List<Note> optionNotes;

    public void setOptionNotes(List<Note> notes) {
        optionNotes = notes;
        optionNoteIds = notes.stream().map(Note::getId).map(Object::toString).collect(Collectors.joining(","));
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionTypeId = questionType.id;
    }

    public QuestionType getQuestionType() {
        return QuestionType.fromId(questionTypeId);
    }

    private QuizQuestionPresenter getPresenter() {
        return this.getQuestionType().presenter.apply(this);
    }

    public String getDescription() {
        return getPresenter().instruction();
    }

    public String getMainTopic() {
       return getPresenter().mainTopic();
    }

    public Map<Link.LinkType, LinkViewed> getHintLinks() {
        return getPresenter().hintLinks();
    }

    public List<Integer> getViceReviewPointIdList() {
        if(Strings.isBlank(viceReviewPointIds)) return null;
        return Arrays.stream(viceReviewPointIds.split(",")).map(Integer::valueOf).collect(Collectors.toList());
    }

    public List<Note> getScope() {
        return List.of(reviewPoint.getSourceNote().getNotebook().getHeadNote());
    }

    public List<Option> getOptions() {
        QuizQuestion.OptionCreator optionCreator = getPresenter().optionCreator();
        return optionNotes.stream().map(optionCreator::optionFromNote).toList();
    }

    public Answer buildAnswer() {
        Answer answer = new Answer();
        answer.setQuestionType(getQuestionType());
        answer.setViceReviewPointIds(getViceReviewPointIdList());
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
