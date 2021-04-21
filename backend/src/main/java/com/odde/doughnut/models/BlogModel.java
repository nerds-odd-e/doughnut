package com.odde.doughnut.models;

import com.odde.doughnut.entities.BlogArticle;
import com.odde.doughnut.entities.BlogYearMonth;
import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.data.repository.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BlogModel extends ModelForEntity<Note> {
    ModelFactoryService modelFactoryService;
    public BlogModel(Note entity, ModelFactoryService modelFactoryService) {
        super(entity, modelFactoryService)        ;
    } {
        //TODO Get the start point for notebook
        int _blogNoteBookId = -1;
        modelFactoryService = modelFactoryService;
        //To find the start notebook
    }

    public List<BlogYearMonth> getBlogYearMonths(int blogNoteBookId) {
        List<BlogYearMonth> result = new ArrayList<BlogYearMonth>();

        return result;
        //Note headNote = modelFactoryService.noteRepository.findById(blogNoteBookId).get();

        //List<Note> notes = headNote.getChildren();


        //return result;
    }
    public List<BlogArticle> getBlogArticles() {

        List<BlogArticle> result = new ArrayList<BlogArticle>();


        return result;
    }
    public void getBlogArticle(int blogNoteBookId) {

    }
}
