package com.odde.doughnut.services.book;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.BookBlock;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BookBlockFlatOutlineWireTest {

  @Test
  void derivesParentAndSiblingOrderForTwoLevelTree() {
    BookBlock root = block(1, 0, 0);
    BookBlock ch0 = block(2, 1, 1);
    BookBlock ch1 = block(3, 2, 1);
    List<BookBlock> blocks = new ArrayList<>(List.of(root, ch0, ch1));

    BookBlockFlatOutlineWire.hydrateBookBlocks(blocks);

    assertThat(root.getParentBlockId(), nullValue());
    assertThat(root.getSiblingOrder(), equalTo(0L));
    assertThat(ch0.getParentBlockId(), equalTo(1));
    assertThat(ch0.getSiblingOrder(), equalTo(0L));
    assertThat(ch1.getParentBlockId(), equalTo(1));
    assertThat(ch1.getSiblingOrder(), equalTo(1L));
  }

  @Test
  void singleChildAfterParentRespectsDepthConstraint() {
    BookBlock root = block(10, 0, 0);
    BookBlock only = block(11, 1, 1);
    List<BookBlock> blocks = new ArrayList<>(List.of(root, only));

    BookBlockFlatOutlineWire.hydrateBookBlocks(blocks);

    assertThat(only.getParentBlockId(), equalTo(10));
    assertThat(only.getSiblingOrder(), equalTo(0L));
  }

  @Test
  void twoRootsAreSiblingsUnderNullParent() {
    BookBlock a = block(1, 0, 0);
    BookBlock b = block(2, 1, 0);
    List<BookBlock> blocks = new ArrayList<>(List.of(a, b));

    BookBlockFlatOutlineWire.hydrateBookBlocks(blocks);

    assertThat(a.getParentBlockId(), nullValue());
    assertThat(b.getParentBlockId(), nullValue());
    assertThat(a.getSiblingOrder(), equalTo(0L));
    assertThat(b.getSiblingOrder(), equalTo(1L));
  }

  private static BookBlock block(int id, int layoutSequence, int depth) {
    BookBlock b = new BookBlock();
    ReflectionTestUtils.setField(b, "id", id);
    b.setLayoutSequence(layoutSequence);
    b.setDepth(depth);
    b.setStructuralTitle("t");
    return b;
  }
}
