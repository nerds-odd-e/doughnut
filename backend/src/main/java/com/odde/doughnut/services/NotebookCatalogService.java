package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.NotebookCatalogGroupItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogNotebookItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogSubscribedNotebookItem;
import com.odde.doughnut.controllers.dto.NotebookClientView;
import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
import com.odde.doughnut.controllers.dto.SubscriptionForNotebooksListing;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookRepository;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class NotebookCatalogService {

  private final BookRepository bookRepository;
  private final NotebookService notebookService;

  public NotebookCatalogService(BookRepository bookRepository, NotebookService notebookService) {
    this.bookRepository = bookRepository;
    this.notebookService = notebookService;
  }

  public NotebookClientView clientViewFor(Notebook notebook, User viewer) {
    boolean hasAttachedBook =
        !bookRepository.findNotebookIdsWithAttachedBooksIn(List.of(notebook.getId())).isEmpty();
    boolean readonly = viewer == null || !viewer.owns(notebook);
    Integer indexNoteId =
        notebookService.findOptionalIndexNote(notebook).map(Note::getId).orElse(null);
    return NotebookClientView.of(notebook, hasAttachedBook, readonly, indexNoteId);
  }

  /**
   * Sort keys for the notebooks page catalog (merged list of ungrouped notebooks and groups):
   *
   * <ul>
   *   <li><b>Notebook</b> row: {@link Notebook#getUpdatedAt()}.
   *   <li><b>Group</b> row: {@link NotebookGroup#getCreatedAt()}.
   * </ul>
   */
  public NotebooksViewedByUser buildView(
      User viewer,
      List<Notebook> allNotebooks,
      List<NotebookGroup> groups,
      List<Subscription> subscriptions) {
    Set<Integer> ids = notebookIdsReferenced(allNotebooks, subscriptions);
    Set<Integer> withBook =
        ids.isEmpty()
            ? Set.of()
            : new HashSet<>(bookRepository.findNotebookIdsWithAttachedBooksIn(ids));

    Map<Integer, NotebookClientView> byNotebookId = new HashMap<>();
    Function<Notebook, NotebookClientView> wrap =
        nb ->
            byNotebookId.computeIfAbsent(
                nb.getId(),
                __ ->
                    NotebookClientView.of(
                        nb,
                        withBook.contains(nb.getId()),
                        viewer == null || !viewer.owns(nb),
                        null));

    NotebooksViewedByUser dto = new NotebooksViewedByUser();
    dto.notebooks = allNotebooks.stream().map(wrap).toList();
    dto.catalogItems = buildCatalogItems(allNotebooks, groups, subscriptions, wrap, withBook);
    dto.subscriptions =
        subscriptions.stream()
            .map(
                s ->
                    SubscriptionForNotebooksListing.from(
                        s, s.getNotebook(), withBook.contains(s.getNotebook().getId())))
            .toList();
    return dto;
  }

  private static Set<Integer> notebookIdsReferenced(
      List<Notebook> allNotebooks, List<Subscription> subscriptions) {
    Set<Integer> ids = new HashSet<>();
    allNotebooks.forEach(n -> ids.add(n.getId()));
    for (Subscription s : subscriptions) {
      if (s.getNotebook() != null) {
        ids.add(s.getNotebook().getId());
      }
    }
    return ids;
  }

  private static List<NotebookCatalogItem> buildCatalogItems(
      List<Notebook> allNotebooks,
      List<NotebookGroup> groups,
      List<Subscription> subscriptions,
      Function<Notebook, NotebookClientView> wrap,
      Set<Integer> withBook) {
    List<SortableRow> rows = new ArrayList<>();

    for (Notebook notebook : allNotebooks) {
      if (notebook.getNotebookGroup() != null) {
        continue;
      }
      rows.add(
          new SortableRow(
              new NotebookCatalogNotebookItem(notebook, withBook.contains(notebook.getId())),
              catalogSortTimestamp(notebook),
              0,
              notebook.getId()));
    }

    for (Subscription subscription : subscriptions) {
      if (subscription.getNotebookGroup() != null) {
        continue;
      }
      Notebook notebook = subscription.getNotebook();
      rows.add(
          new SortableRow(
              new NotebookCatalogSubscribedNotebookItem(
                  notebook, subscription.getId(), withBook.contains(notebook.getId())),
              catalogSortTimestamp(notebook),
              0,
              notebook.getId()));
    }

    for (NotebookGroup group : groups) {
      List<Notebook> members =
          allNotebooks.stream()
              .filter(
                  nb ->
                      nb.getNotebookGroup() != null
                          && Objects.equals(nb.getNotebookGroup().getId(), group.getId()))
              .sorted(
                  Comparator.comparing(NotebookCatalogService::catalogSortTimestamp)
                      .thenComparing(Notebook::getId))
              .toList();
      List<Notebook> subscribedMembers =
          subscriptions.stream()
              .filter(
                  s ->
                      s.getNotebookGroup() != null
                          && Objects.equals(s.getNotebookGroup().getId(), group.getId()))
              .sorted(
                  Comparator.comparing((Subscription s) -> catalogSortTimestamp(s.getNotebook()))
                      .thenComparing(s -> s.getNotebook().getId()))
              .map(Subscription::getNotebook)
              .toList();
      List<Notebook> allMembers = new ArrayList<>(members);
      allMembers.addAll(subscribedMembers);
      allMembers.sort(
          Comparator.comparing(NotebookCatalogService::catalogSortTimestamp)
              .thenComparing(Notebook::getId));
      rows.add(
          new SortableRow(
              new NotebookCatalogGroupItem(
                  group.getId(),
                  group.getName(),
                  group.getCreatedAt(),
                  allMembers.stream().map(wrap).toList()),
              group.getCreatedAt(),
              1,
              group.getId()));
    }

    rows.sort(
        Comparator.comparing((SortableRow r) -> r.sortAt)
            .thenComparing(r -> r.kind)
            .thenComparing(r -> r.tieId));

    return rows.stream().map(r -> r.item).toList();
  }

  private record SortableRow(NotebookCatalogItem item, Timestamp sortAt, int kind, int tieId) {}

  private static Timestamp catalogSortTimestamp(Notebook notebook) {
    Timestamp at = notebook.getUpdatedAt();
    return at != null ? at : new Timestamp(0L);
  }
}
