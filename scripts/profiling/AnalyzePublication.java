import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/** Summarizes a selected JFR event-time span and publication thread. */
public class AnalyzePublication {
  static Map<String, Long> counts = new TreeMap<>();

  static void add(String key, long value) {
    counts.merge(key, value, Long::sum);
  }

  static void top(String prefix, int limit) {
    counts.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(prefix))
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(limit)
        .forEach(entry -> System.out.println(entry.getKey() + "\t" + entry.getValue()));
  }

  static String frames(RecordedEvent event) {
    if (event.getStackTrace() == null) return "";
    StringBuilder stack = new StringBuilder();
    for (var frame : event.getStackTrace().getFrames()) {
      stack.append(frame.getMethod().getType().getName())
          .append('.').append(frame.getMethod().getName()).append('\n');
    }
    return stack.toString();
  }

  public static void main(String[] args) throws Exception {
    var start = Instant.parse(args[1]);
    var end = Instant.parse(args[2]);
    var midpoint = start.plusMillis(Duration.between(start, end).toMillis() / 2);
    String thread = args.length > 3 ? args[3] : "";
    Set<String> settings = new TreeSet<>();
    Instant first = null, last = null, sampleFirst = null, sampleLast = null;
    var finalSamples = new ArrayDeque<String>();
    var eventLast = new TreeMap<String, Instant>();

    try (var recording = new RecordingFile(Path.of(args[0]))) {
      var types = new HashMap<Long, String>();
      for (var type : recording.readEventTypes()) types.put(type.getId(), type.getName());
      while (recording.hasMoreEvents()) {
        var event = recording.readEvent();
        var name = event.getEventType().getName();
        var at = event.getStartTime();
        if (first == null || at.isBefore(first)) first = at;
        if (last == null || at.isAfter(last)) last = at;
        if (name.equals("jdk.ActiveSetting")) {
          String type = types.get(event.getLong("id"));
          if (type != null && (type.contains("Socket") || type.contains("ThreadPark")
              || type.contains("JavaMonitorEnter") || type.contains("ExecutionSample")
              || type.contains("ObjectAllocationSample"))) {
            settings.add(type + " " + event.getString("name") + "=" + event.getString("value"));
          }
        }
        eventLast.merge(name, at, (a, b) -> a.isAfter(b) ? a : b);
        if (name.equals("jdk.ActiveRecording")) System.out.println("activeRecording=" + event);
        if (at.isBefore(start) || at.isAfter(end)) continue;
        add("event." + name, 1);

        if (name.equals("jdk.ExecutionSample")) {
          String threadName = event.getThread("sampledThread").getJavaName();
          add("thread." + threadName, 1);
          String stack = frames(event);
          if (stack.contains("NotebookGitProposal")) add("publisherThread." + threadName, 1);
          if (!threadName.equals(thread)) continue;
          if (sampleFirst == null) sampleFirst = at;
          sampleLast = at;
          finalSamples.add(at + "\n" + stack);
          if (finalSamples.size() > 3) finalSamples.remove();
          String part = at.isBefore(midpoint) ? "early" : "late";
          String[] stackFrames = stack.split("\n");
          if (stackFrames.length > 0) add("leaf." + stackFrames[0], 1);
          add(part + ".samples", 1);
          for (String term : List.of("org.hibernate", "org.yaml.snakeyaml", "org.eclipse.jgit",
              "NotePropertyIndexService", "NoteAliasIndexService", "AuthoredNote",
              "NotebookGitProjection", "DefaultFlush", "AbstractFlushingEventListener",
              "org.hibernate.engine.internal.Cascade", "com.mysql")) {
            if (stack.contains(term)) {
              add("stack." + term, 1);
              add(part + "." + term, 1);
            }
          }
          if (event.getStackTrace() != null && event.getStackTrace().isTruncated()) {
            add("truncated.execution", 1);
          }
        }
        if (name.equals("jdk.ObjectAllocationSample")) {
          long weight = event.getLong("weight");
          add("allocation.all.weightBytes", weight);
          if (event.getThread().getJavaName().equals(thread)) {
            add("allocation.request.weightBytes", weight);
            add("allocation.class." + event.getClass("objectClass").getName(), weight);
          }
        }
        if (name.equals("jdk.GarbageCollection")) {
          add("gc.durationNanos", event.getDuration().toNanos());
          add("gc.sumOfPausesNanos", event.getDuration("sumOfPauses").toNanos());
          add("gc.cause." + event.getString("cause"), 1);
        }
        if (name.equals("jdk.GCPhasePause")) add("gc.pauseEventNanos", event.getDuration().toNanos());
        if (List.of("jdk.SocketRead", "jdk.SocketWrite", "jdk.ThreadPark", "jdk.JavaMonitorEnter")
            .contains(name)) {
          add("wait.all." + name + ".nanos", event.getDuration().toNanos());
          if (event.getThread() != null && event.getThread().getJavaName().equals(thread)) {
            add("wait.request." + name + ".count", 1);
            add("wait.request." + name + ".nanos", event.getDuration().toNanos());
          }
        }
      }
    }
    System.out.println("recordingFirst=" + first + " recordingLast=" + last
        + " requestStart=" + start + " requestEnd=" + end + " midpoint=" + midpoint
        + " selectedThread=" + thread);
    System.out.println("sampleFirst=" + sampleFirst + " sampleLast=" + sampleLast);
    eventLast.forEach((key, value) -> System.out.println("last." + key + "=" + value));
    finalSamples.forEach(sample -> System.out.println("finalSample=" + sample));
    for (String prefix : List.of("publisherThread.", "thread.", "leaf.", "stack.", "early.",
        "late.", "truncated.", "allocation.class.")) {
      top(prefix, 20);
    }
    counts.forEach((key, value) -> {
      if (key.startsWith("event.") || key.startsWith("gc.") || key.startsWith("wait.")
          || key.equals("allocation.all.weightBytes") || key.equals("allocation.request.weightBytes")) {
        System.out.println(key + "\t" + value);
      }
    });
    settings.forEach(System.out::println);
  }
}
