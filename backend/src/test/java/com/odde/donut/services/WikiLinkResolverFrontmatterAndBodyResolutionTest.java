package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.algorithms.NoteReferenceResolution;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WikiLinkResolverFrontmatterAndBodyResolutionTest {

  @Autowired MakeMe makeMe;
  @Autowired WikiLinkResolver wikiLinkResolver;

  @Test
  void resolvesReferencesFromFrontmatterAndBody() {
    User owner = makeMe.aUser().please();
    Note frontmatterTarget =
        makeMe.aNote().title("Frontmatter Target").notebookOwnedBy(owner).please();
    Note bodyTarget =
        makeMe.aNote().title("Body Target").underSameNotebookAs(frontmatterTarget).please();
    Note source =
        makeMe
            .aNote()
            .underSameNotebookAs(frontmatterTarget)
            .content(
                """
                ---
                parent: "[[Frontmatter Target]]"
                ---
                See [[Body Target]].
                """)
            .please();

    List<Integer> resolvedNoteIds =
        AuthoredNoteReferences.inOccurrenceOrder(
                source.getContent(), wikiLinkResolver.canonicalDonutOrigin())
            .stream()
            .map(reference -> wikiLinkResolver.resolveReference(reference, source, owner))
            .map(NoteReferenceResolution.Resolved.class::cast)
            .map(resolved -> resolved.destinationNote().getId())
            .toList();

    assertThat(resolvedNoteIds, contains(frontmatterTarget.getId(), bodyTarget.getId()));
  }
}
