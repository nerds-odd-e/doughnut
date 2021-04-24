package com.odde.doughnut.models;

import com.odde.doughnut.entities.BlogArticle;
import com.odde.doughnut.entities.BlogYearMonth;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BlogModel extends ModelForEntity<Note> {
    public BlogModel(Note entity, ModelFactoryService modelFactoryService) {
        super(entity, modelFactoryService)        ;
    }

    public List<BlogYearMonth> getBlogYearMonths(int blogNoteBookId) {

        NoteRepository noteRepository = modelFactoryService.noteRepository;
        Optional<Note> byId = noteRepository.findById(blogNoteBookId);
        Note headNote = byId.get();

        return getBlogYearMonths(headNote);
    }

    public List<BlogYearMonth> getBlogYearMonths(Note headNote) {
        List<BlogYearMonth> years = new ArrayList<>();

        List<Note> noteYears = headNote.getChildren();

        noteYears.forEach(note-> years.add(new BlogYearMonth(note.getTitle(), "Jan"))); //ToDo for dynamic month

        return years;
    }

    public List<BlogArticle> getBlogPosts(Note parentNote) {
        if (parentNote == null) {
            return new ArrayList<>();
        }
        return parentNote.getChildren().stream()
                .map(this::toBlogPost)
                .collect(Collectors.toList());
    }

    public BlogArticle toBlogPost(Note note) {
        BlogArticle article = new BlogArticle();
        article.setTitle(note.getTitle().split(": ")[1]);
        article.setDescription(note.getNoteContent().getDescription());
        article.setAuthor(note.getUser().getName());
        article.setCreatedDatetime(note.getTitle().split(": ")[0]);
        return article;
    }

}
