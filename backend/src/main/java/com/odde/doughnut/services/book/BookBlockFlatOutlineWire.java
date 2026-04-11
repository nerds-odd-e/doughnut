package com.odde.doughnut.services.book;

import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BookBlockFlatOutlineWire {

  private BookBlockFlatOutlineWire() {}

  public static void hydrateBook(Book book) {
    hydrateBookBlocks(book.getBlocks());
  }

  public static void hydrateBookBlocks(List<BookBlock> blocks) {
    if (blocks.isEmpty()) {
      return;
    }
    List<BookBlock> ordered =
        blocks.stream().sorted(Comparator.comparingInt(BookBlock::getLayoutSequence)).toList();

    Deque<BookBlock> stack = new ArrayDeque<>();
    for (BookBlock b : ordered) {
      while (!stack.isEmpty() && stack.peek().getDepth() >= b.getDepth()) {
        stack.pop();
      }
      Integer parentId = stack.isEmpty() ? null : stack.peek().getId();
      b.setWireParentBlockId(parentId);
      stack.push(b);
    }

    long rootNext = 0;
    Map<Integer, Long> nextByParent = new HashMap<>();
    for (BookBlock b : ordered) {
      Integer pid = b.getWireParentBlockId();
      if (pid == null) {
        b.setWireSiblingOrder(rootNext++);
      } else {
        long n = nextByParent.getOrDefault(pid, 0L);
        b.setWireSiblingOrder(n);
        nextByParent.put(pid, n + 1);
      }
    }
  }
}
