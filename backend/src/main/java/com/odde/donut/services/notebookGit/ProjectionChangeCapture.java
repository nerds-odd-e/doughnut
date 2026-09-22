package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.EntityIdentifiedByIdOnly;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hibernate.Interceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.Type;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Records, while a capture window is open on the current thread, which projection rows (notes,
 * folders, attachments, notebook readmes) the flushed changes inserted, updated or deleted, grouped
 * by notebook. Updates keep the first-seen previous path fields so a later flush cannot lose the
 * original path. Attachment bytes are never read.
 */
@Component
public class ProjectionChangeCapture implements Interceptor, HibernatePropertiesCustomizer {

  public record ProjectionRow(Class<?> kind, Integer id) {}

  /** Container is the folder (notes, attachments) or parent folder (folders); null at root. */
  public record RowPath(Integer containerId, String name) {}

  public static class NotebookProjectionChange {
    public final Set<ProjectionRow> inserted = new LinkedHashSet<>();
    public final Map<ProjectionRow, RowPath> updated = new LinkedHashMap<>();
    public final Set<ProjectionRow> deleted = new LinkedHashSet<>();
  }

  public class ProjectionChange implements AutoCloseable {
    private final Map<Integer, NotebookProjectionChange> byNotebook = new HashMap<>();

    public NotebookProjectionChange of(Integer notebookId) {
      return byNotebook.computeIfAbsent(notebookId, id -> new NotebookProjectionChange());
    }

    @Override
    public void close() {
      window.remove();
    }
  }

  /** Property names of a row's path fields: its container and its display name. */
  private record PathFields(String container, String name) {}

  private static final Map<Class<?>, PathFields> PATH_FIELDS =
      Map.of(
          Note.class, new PathFields("folder", "title"),
          Folder.class, new PathFields("parentFolder", "name"),
          NotebookAttachment.class, new PathFields("folder", "filename"));

  private final ThreadLocal<ProjectionChange> window = new ThreadLocal<>();

  public ProjectionChange open() {
    ProjectionChange change = new ProjectionChange();
    window.set(change);
    return change;
  }

  @Override
  public void customize(Map<String, Object> hibernateProperties) {
    hibernateProperties.put(AvailableSettings.INTERCEPTOR, this);
  }

  @Override
  public boolean onSave(Object entity, Object id, Object[] state, String[] names, Type[] types) {
    NotebookProjectionChange change = changeFor(entity, state, names);
    if (change != null) change.inserted.add(row(entity, id));
    return false;
  }

  @Override
  public void onDelete(Object entity, Object id, Object[] state, String[] names, Type[] types) {
    NotebookProjectionChange change = changeFor(entity, state, names);
    if (change != null) change.deleted.add(row(entity, id));
  }

  @Override
  public boolean onFlushDirty(
      Object entity, Object id, Object[] current, Object[] previous, String[] names, Type[] types) {
    NotebookProjectionChange change = changeFor(entity, previous, names);
    if (change != null) {
      RowPath previousPath = previousPath(entity, current, previous, names);
      if (previousPath != null) change.updated.putIfAbsent(row(entity, id), previousPath);
    }
    return false;
  }

  /** The window's record for the entity's notebook; null when no window is open or not tracked. */
  private NotebookProjectionChange changeFor(Object entity, Object[] state, String[] names) {
    ProjectionChange change = window.get();
    if (change == null) return null;
    if (entity instanceof Notebook notebook) return change.of(notebook.getId());
    if (!PATH_FIELDS.containsKey(entity.getClass())) return null;
    return change.of(idAt(state, names, "notebook"));
  }

  /** A notebook row counts as a path change only when its root README content changed. */
  private static RowPath previousPath(
      Object entity, Object[] current, Object[] previous, String[] names) {
    if (entity instanceof Notebook) {
      boolean readmeUnchanged =
          Objects.equals(at(previous, names, "readmeContent"), at(current, names, "readmeContent"));
      return readmeUnchanged ? null : new RowPath(null, null);
    }
    PathFields fields = PATH_FIELDS.get(entity.getClass());
    return new RowPath(
        idAt(previous, names, fields.container()), nameAt(previous, names, fields.name()));
  }

  private static ProjectionRow row(Object entity, Object id) {
    return new ProjectionRow(entity.getClass(), (Integer) id);
  }

  private static Object at(Object[] state, String[] names, String name) {
    return state[Arrays.asList(names).indexOf(name)];
  }

  private static Integer idAt(Object[] state, String[] names, String name) {
    Object value = at(state, names, name);
    return value == null ? null : ((EntityIdentifiedByIdOnly) value).getId();
  }

  private static String nameAt(Object[] state, String[] names, String name) {
    Object value = at(state, names, name);
    return value instanceof DisplayName displayName ? displayName.value() : (String) value;
  }
}
