package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.QuizQuestion;

import java.util.List;

public class DefaultQuizFactory implements QuizQuestionFactory {
    private final ReviewPoint reviewPoint;
    private final Note answerNote;
    private final QuizQuestionServant servant;

    public DefaultQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.reviewPoint = reviewPoint;
        this.servant = servant;
        this.answerNote = getAnswerNote();
    }

    @Override
    public List<Note> generateFillingOptions() {
        return servant.choose5FromSiblings(answerNote, n -> !n.equals(answerNote));
    }

    @Override
    public String generateInstruction() {
        return answerNote.getNoteContent().getClozeDescription();
    }

    @Override
    public String generateMainTopic() {
        return "";
    }

    @Override
    public Note generateAnswerNote() {
        return answerNote;
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<Note> noteEntities) {
        return servant.toTitleOptions(noteEntities);
    }

    private Note getAnswerNote() {
        return reviewPoint.getNote();
    }

}