package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import org.apache.logging.log4j.util.Strings;

import java.util.List;

public class ClozeTitleSelectionQuizFactory extends ClozeDescriptonQuizFactory {
    public ClozeTitleSelectionQuizFactory(ReviewPoint reviewPoint) {
        super(reviewPoint);
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        return servant.choose5FromSiblings(answerNote, n -> !n.equals(answerNote));
    }

    @Override
    public boolean isValidQuestion() {
        return !Strings.isEmpty(reviewPoint.getNote().getNoteContent().getDescription());
    }
}