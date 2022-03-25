package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.quizFacotries.QuizQuestionPresenter;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class QuizQuestionViewedByUser {

    public QuizQuestion quizQuestion;

    @Getter
    public QuizQuestion.QuestionType questionType;

    @Getter
    public String description;

    @Getter
    public String mainTopic;

    @Getter
    public Map<Link.LinkType, LinkViewed> hintLinks;

    @Getter
    public List<Integer> viceReviewPointIdList;

    public List<Note> scope;

    @Getter
    public List<Option> options;

    public Optional<PictureWithMask> pictureWithMask;

    public Optional<Integer> revealedNoteId;

    public static QuizQuestionViewedByUser from(QuizQuestion quizQuestion, NoteRepository noteRepository) {
        QuizQuestionPresenter presenter = quizQuestion.getQuestionType().presenter.apply(quizQuestion);
        QuizQuestionViewedByUser question = new QuizQuestionViewedByUser();
        question.quizQuestion = quizQuestion;
        question.questionType = quizQuestion.getQuestionType();
        question.description = presenter.instruction();
        question.mainTopic = presenter.mainTopic();
        question.hintLinks = presenter.hintLinks();
        question.pictureWithMask = presenter.pictureWithMask();
        question.revealedNoteId = presenter.revealedNoteId();
        question.viceReviewPointIdList = quizQuestion.getViceReviewPointIdList();
        question.scope = List.of(quizQuestion.getReviewPoint().getSourceNote().getNotebook().getHeadNote());
        Stream<Note> noteStream = noteRepository.findAllByIds(quizQuestion.getOptionNoteIds().split(","));
        question.options = noteStream.map(presenter.optionCreator()::optionFromNote).toList();
        return question;
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
