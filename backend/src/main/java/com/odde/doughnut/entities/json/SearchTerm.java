package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.EntityIdResolver;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

public class SearchTerm {
    @Setter
    private String searchKey = "";

    @Getter
    @Setter
    private Boolean searchGlobally = false;

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            resolver = EntityIdResolver.class,
            scope = Note.class,
            property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    public Optional<Note> note;

    @JsonIgnore
    public String getTrimmedSearchKey() {
        return searchKey.trim();
    }
}
