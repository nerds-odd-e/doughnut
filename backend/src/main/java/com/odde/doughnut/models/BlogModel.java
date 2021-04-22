package com.odde.doughnut.models;

import com.odde.doughnut.entities.BlogArticle;
import com.odde.doughnut.entities.BlogYearMonth;
import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.data.repository.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        List<BlogYearMonth> years = new ArrayList<BlogYearMonth>();

        List<Note> noteYears = headNote.getChildren();

        noteYears.forEach(note-> years.add(new BlogYearMonth(note.getTitle(), "Jan"))); //ToDo for dynamic month

        return years;
    }

    public List<BlogArticle> getBlogArticles() {

        List<BlogArticle> result = new ArrayList<BlogArticle>();


        return result;
    }
    public void getBlogArticle(int blogNoteBookId) {

    }
}
