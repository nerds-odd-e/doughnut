package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteRealmServiceTest {

  @Autowired com.odde.doughnut.testability.MakeMe makeMe;
  @Autowired NoteRealmService noteRealmService;
  @Autowired NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;

  @Test
  void wiki_titles_empty_when_details_have_links_but_cache_not_refreshed() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    makeMe.aNote().title("LinkedPage").under(root).please();
    Note carrier = makeMe.aNote().under(root).details("[[LinkedPage]]").please();

    NoteRealm realm = noteRealmService.build(carrier, user);

    assertThat(realm.getWikiTitles(), empty());
  }

  @Test
  void omits_cached_target_when_viewer_cannot_read_target_notebook() {
    User otherUser = makeMe.aUser().please();
    Note headSecret = makeMe.aNote().creatorAndOwner(otherUser).title("SecretNb").please();
    Note hidden = makeMe.aNote().title("Hidden").under(headSecret).please();

    User viewer = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(viewer).please();
    Note carrier = makeMe.aNote().under(root).details("plain").please();

    NoteWikiTitleCache row = new NoteWikiTitleCache();
    row.setNote(carrier);
    row.setTargetNote(hidden);
    row.setLinkText("SecretNb:Hidden");
    noteWikiTitleCacheRepository.save(row);

    NoteRealm realm = noteRealmService.build(carrier, viewer);

    assertThat(realm.getWikiTitles(), empty());
  }
}
