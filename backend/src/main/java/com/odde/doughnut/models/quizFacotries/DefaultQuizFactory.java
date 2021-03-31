package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.QuizQuestion;

import java.util.List;

public class DefaultQuizFactory implements QuizQuestionFactory {
    private final ReviewPointEntity reviewPointEntity;
    private final NoteEntity answerNote;
    private final QuizQuestionServant servant;

    public DefaultQuizFactory(QuizQuestionServant servant, ReviewPointEntity reviewPointEntity) {
        this.reviewPointEntity = reviewPointEntity;
        this.servant = servant;
        this.answerNote = getAnswerNote();
    }

    @Override
    public List<NoteEntity> generateFillingOptions() {
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
    public NoteEntity generateAnswerNote() {
        return answerNote;
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<NoteEntity> noteEntities) {
        return servant.toTitleOptions(noteEntities);
    }

    private NoteEntity getAnswerNote() {
        return reviewPointEntity.getNoteEntity();
    }

}