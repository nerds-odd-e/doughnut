package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class SearchControllerTestBase extends ControllerTestBase {
  @Autowired SearchController controller;

  @BeforeEach
  void setupSearchBase() {
    currentUser.setUser(makeMe.aUser().please());
  }

  Note ownedNote(String title) {
    return makeMe.aNote(title).notebookOwnedBy(currentUser.getUser()).please();
  }

  SearchTerm searchTerm(String searchKey) {
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey(searchKey);
    searchTerm.setAllMyNotebooksAndSubscriptions(true);
    return searchTerm;
  }

  SearchTerm searchTermInMyNotebooksOnly(String searchKey) {
    SearchTerm searchTerm = searchTerm(searchKey);
    searchTerm.setAllMyCircles(false);
    return searchTerm;
  }

  SearchTerm searchTermWithoutGlobalScope(String searchKey) {
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey(searchKey);
    return searchTerm;
  }
}
