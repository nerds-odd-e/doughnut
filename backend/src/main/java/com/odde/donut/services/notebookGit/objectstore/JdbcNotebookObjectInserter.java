package com.odde.donut.services.notebookGit.objectstore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.transport.PackParser;
import org.eclipse.jgit.util.IO;

/**
 * Buffers every object written within one inserter session in memory, keyed by object ID (so
 * repeated inserts of identical content within a session are free), and defers all database work to
 * {@link #flush()}. This is what turns a {@code DirCacheBuilder}-triggered save - which reattempts
 * an {@code insert()} for every tree in the hierarchy, not just the changed path - into one batched
 * existence check plus one batched insert of whatever is genuinely new, instead of one round trip
 * per attempted object.
 */
final class JdbcNotebookObjectInserter extends ObjectInserter {

  record BufferedObject(int type, byte[] data) {}

  private final JdbcNotebookObjectDatabase database;
  private final Map<ObjectId, BufferedObject> buffered = new LinkedHashMap<>();

  JdbcNotebookObjectInserter(JdbcNotebookObjectDatabase database) {
    this.database = database;
  }

  @Override
  public ObjectId insert(int type, byte[] data, int off, int len) {
    byte[] copy = Arrays.copyOfRange(data, off, off + len);
    ObjectId id = idFor(type, copy);
    buffered.putIfAbsent(id, new BufferedObject(type, copy));
    return id;
  }

  @Override
  public ObjectId insert(int objectType, long length, InputStream in) throws IOException {
    if (length > Integer.MAX_VALUE) {
      throw new IOException("Object too large for this store: " + length + " bytes");
    }
    byte[] data = new byte[(int) length];
    IO.readFully(in, data, 0, (int) length);
    return insert(objectType, data, 0, data.length);
  }

  @Override
  public PackParser newPackParser(InputStream in) {
    throw new UnsupportedOperationException(
        "This store never parses inbound pack streams; objects are written through insert(...)"
            + " calls from JGit's own higher-level APIs.");
  }

  @Override
  public ObjectReader newReader() {
    return new JdbcNotebookObjectReader(database, buffered);
  }

  @Override
  public void flush() throws IOException {
    database.insertMissing(buffered);
  }

  @Override
  public void close() {
    buffered.clear();
  }
}
