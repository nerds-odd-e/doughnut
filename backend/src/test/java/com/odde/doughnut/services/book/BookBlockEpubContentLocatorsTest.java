package com.odde.doughnut.services.book;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.doughnut.entities.BookContentBlock;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookBlockEpubContentLocatorsTest {

  @Test
  void rawFragmentWithLeadingHashProducesBareIdInLocator() {
    BookContentBlock b =
        block(
            "{\"type\":\"text\",\"href\":\"OEBPS/c.xhtml\",\"fragment\":\"#sec\",\"text\":\"t\"}");
    List<ContentLocator> locs = BookBlockEpubContentLocators.epubContentLocators(List.of(b));
    assertThat(locs, hasSize(1));
    EpubLocator loc = (EpubLocator) locs.getFirst();
    assertThat(loc.href(), equalTo("OEBPS/c.xhtml"));
    assertThat(loc.fragment(), equalTo("sec"));
  }

  @Test
  void rawFragmentBareIdUnchangedInLocator() {
    BookContentBlock b =
        block("{\"type\":\"text\",\"href\":\"OEBPS/c.xhtml\",\"fragment\":\"sec\",\"text\":\"t\"}");
    List<ContentLocator> locs = BookBlockEpubContentLocators.epubContentLocators(List.of(b));
    assertThat(locs, hasSize(1));
    EpubLocator loc = (EpubLocator) locs.getFirst();
    assertThat(loc.href(), equalTo("OEBPS/c.xhtml"));
    assertThat(loc.fragment(), equalTo("sec"));
  }

  private static BookContentBlock block(String rawJson) {
    BookContentBlock cb = new BookContentBlock();
    cb.setType("text");
    cb.setRawData(rawJson);
    return cb;
  }
}
