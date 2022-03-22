package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.NoteViewer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ClozeDescriptonQuizFactory implements QuizQuestionFactory {
    protected final ReviewPoint reviewPoint;
    protected final Note answerNote;

    public ClozeDescriptonQuizFactory(ReviewPoint reviewPoint) {
        this.reviewPoint = reviewPoint;
        this.answerNote = getAnswerNote();
    }

    @Override
    public String generateInstruction() {
        return answerNote.getClozeDescription();
    }

    @Override
    public String generateMainTopic() {
        return "";
    }

    @Override
    public Note generateAnswerNote(QuizQuestionServant servant) {
        return answerNote;
    }

    @Override
    public List<Note> knownRightAnswers() {
        return List.of(answerNote);
    }

    private Note getAnswerNote() {
        return reviewPoint.getNote();
    }

}