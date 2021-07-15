package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import org.apache.logging.log4j.util.Strings;

import java.util.List;

public class ClozeTitleSelectionQuizFactory extends ClozeDescriptonQuizFactory {
    public ClozeTitleSelectionQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        super(servant, reviewPoint);
    }

    @Override
    public List<Note> generateFillingOptions() {
        return servant.choose5FromSiblings(answerNote, n -> !n.equals(answerNote));
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<Note> noteEntities) {
        return servant.toTitleOptions(noteEntities);
    }

    @Override
    public boolean isValidQuestion() {
        return !Strings.isEmpty(reviewPoint.getNote().getNoteContent().getDescription());
    }
}