package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.QuizQuestion;

import java.util.List;

public class PictureSelectionQuizFactory implements QuizQuestionFactory {
    private final Note answerNote;
    private final QuizQuestionServant servant;

    public PictureSelectionQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.servant = servant;
        this.answerNote = reviewPoint.getNote();
    }

    @Override
    public List<Note> generateFillingOptions() {
        return servant.choose5FromSiblings(answerNote, n -> n.getNoteContent().hasPicture() && !n.equals(answerNote));
    }

    @Override
    public String generateInstruction() {
        return "";
    }

    @Override
    public String generateMainTopic() {
        return answerNote.getTitle();
    }

    @Override
    public Note generateAnswerNote() {
        return answerNote;
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<Note> noteEntities) {
        return servant.toPictureOptions(noteEntities);
    }

}