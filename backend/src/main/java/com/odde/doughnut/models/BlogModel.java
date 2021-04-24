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
        List<BlogYearMonth> years = new ArrayList<BlogYearMonth>();

        List<Note> noteYears = headNote.getChildren();

        noteYears.forEach(note-> years.add(new BlogYearMonth(note.getTitle(), "Jan"))); //ToDo for dynamic month

        return years;
    }

    public List<BlogArticle> getBlogPosts(Note parentNote, BlogYearMonth targetYearMonth) {
        if (parentNote == null) {
            return new ArrayList<>();
        }
        return parentNote.getGreatGreatGrandChildren().stream()
                .filter(post -> isRealBlogPost(post, targetYearMonth))
                .map(Note::toBlogPost)
                .collect(Collectors.toList());
    }

    private boolean isRealBlogPost(Note note, BlogYearMonth targetYearMonth){
        BlogYearMonth articleYearMonth = getArticleYearMonth(note);
        return articleYearMonth.getYear().equals(targetYearMonth.getYear()) && articleYearMonth.getMonth().equals(targetYearMonth.getMonth());

    }

    private BlogYearMonth getArticleYearMonth(Note article) {

        if (article == null) {
            return new BlogYearMonth("all","all");
        }
        Note date = article.getParentNote();
        Note month = article.getParentNote().getParentNote();
        Note year = article.getParentNote().getParentNote().getParentNote();

        return new BlogYearMonth(year.getTitle(), month.getTitle());

    }

}
