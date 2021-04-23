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

    public List<BlogArticle> getBlogArticles(Note parentNote, BlogYearMonth targetYearMonth) {
        return getArticlesFromHeadNote(parentNote, targetYearMonth);
    }

    private List<BlogArticle> getArticlesFromHeadNote(Note parentNote, BlogYearMonth targetYearMonth){
        List<BlogArticle> articles = new ArrayList<BlogArticle>();
        if(parentNote !=null){
            articles.addAll(parentNote.getGreatGreatGrandChildren().stream().filter(article -> isRealArticle(article, targetYearMonth)).map(note -> note.toBlogArticle()).collect(Collectors.toList()));
        }
        return articles;
    }

    private boolean isRealArticle(Note note, BlogYearMonth targetYearMonth){
        BlogYearMonth articleYearMonth = getArticleYearMonth(note);
        return articleYearMonth.getYear().equals(targetYearMonth.getYear()) && articleYearMonth.getMonth().equals(targetYearMonth.getMonth());

    }

    private BlogYearMonth getArticleYearMonth(Note article) {

        if(article != null) {
            Note date = article.getParentNote();
            Note month = article.getParentNote().getParentNote();
            Note year = article.getParentNote().getParentNote().getParentNote();

            BlogYearMonth yearMonth = new BlogYearMonth(year.getTitle(), month.getTitle());

            return yearMonth;
        }

        return new BlogYearMonth("all","all");
    }

}
