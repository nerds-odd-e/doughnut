package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.models.QuizQuestion;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuizQuestionServant {
    private final Randomizer randomizer;
    private final ModelFactoryService modelFactoryService;

    public QuizQuestionServant(Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.randomizer = randomizer;
        this.modelFactoryService = modelFactoryService;
    }

    List<QuizQuestion.Option> toPictureOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createPictureOption).collect(Collectors.toUnmodifiableList());
    }

    List<QuizQuestion.Option> toTitleOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createTitleOption).collect(Collectors.toUnmodifiableList());
    }

    List<NoteEntity> choose5FromSiblings(NoteEntity answerNote, Randomizer randomizer, Predicate<NoteEntity> noteEntityPredicate) {
        List<NoteEntity> siblings = modelFactoryService.toTreeNodeModel(answerNote).getSiblings();
        Stream<NoteEntity> noteEntityStream = siblings.stream()
                .filter(noteEntityPredicate);
        List<NoteEntity> list = noteEntityStream.collect(Collectors.toList());
        return randomizer.randomlyChoose(5, list);
    }


}