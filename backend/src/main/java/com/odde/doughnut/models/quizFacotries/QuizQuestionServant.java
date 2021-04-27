package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.factoryServices.ModelFactoryService;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuizQuestionServant {
    final Randomizer randomizer;
    final ModelFactoryService modelFactoryService;

    public QuizQuestionServant(Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.randomizer = randomizer;
        this.modelFactoryService = modelFactoryService;
    }

    List<QuizQuestion.Option> toPictureOptions(List<Note> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createPictureOption).collect(Collectors.toUnmodifiableList());
    }

    List<QuizQuestion.Option> toTitleOptions(List<Note> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createTitleOption).collect(Collectors.toUnmodifiableList());
    }

    List<Note> choose5FromSiblings(Note answerNote, Predicate<Note> notePredicate) {
        List<Note> siblings = answerNote.getSiblings();
        Stream<Note> noteStream = siblings.stream()
                .filter(notePredicate);
        List<Note> list = noteStream.collect(Collectors.toList());
        return randomizer.randomlyChoose(5, list);
    }

    List<Note> randomlyChooseAndEnsure(List<Note> candidates, Note ensure, int maxSize) {
        List<Note> list = candidates.stream()
                .filter(n -> !n.equals(ensure)).collect(Collectors.toList());
        List<Note> selectedList = this.randomizer.randomlyChoose(maxSize - 1, list);
        selectedList.add(ensure);
        return selectedList;
    }
}