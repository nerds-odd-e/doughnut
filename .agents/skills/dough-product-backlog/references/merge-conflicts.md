# Resolve product backlog merge conflicts

Within the calling workflow's authorization, resolve compatible backlog changes
without asking for confirmation.

1. Compare both backlog versions with their common ancestor to identify each
   side's changes. Match work by established story or correction identity,
   including known story-to-plan links; a shared seed ID is insufficient.
   Consult affected history or canonical homes only if identity or intent is
   unclear.
2. Combine compatible changes from both sides. Apply identical changes once.
   An unchanged entry does not override the other side's take or removal.
   For example, taking A and removing completed B yields A in **Taken** and B
   absent. Different changes to the same work require compatible intentions;
   neither removal nor a later lifecycle state automatically wins.
3. Preserve unrelated titles, links, direction text, and queue order. Retain
   compatible explicit reprioritization; taking or removing work does not
   reprioritize the remaining queue.
4. In **Taken**, retain surviving existing entries in order, then append new
   entries while preserving each side's addition order. Unless the project
   supplies a convention, interleave concurrent additions by repeatedly choosing
   the lexically smallest established identity among the next entries from each
   side, emitting each identity once. Do not use this rule for queue priority.
5. If identity, incompatible changes, or competing order remains unresolved,
   preserve the conflict and ask the human for the specific missing decision.
   For example, removal versus an explicit return to the queue requires a
   decision when available context does not establish which intent applies.
6. Verify both sides' intended changes are represented, each active identity
   appears once across both lists, removed work stays absent, and references
   remain coherent. Retain both section headings even when empty. Report the
   resolved transitions briefly and continue the calling workflow; backlog-only
   resolution needs no implementation test run.
