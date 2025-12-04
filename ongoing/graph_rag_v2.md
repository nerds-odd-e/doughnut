This plan is to implment `docs/graph RAG/graph_RAG_requirements_v2.md`

## step 1: empty endpoint with disabled test

create a new NoteGraphService having the same signature as the retrieve from GraphRAGService but minimum implementation:

```
  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes, tokenCountingStrategy);
    return builder.build();
  }
```

Change the subject of NoteGraphServiceTest to NoteGraphService. Run all the unit test and disable all the test that fails.

## step 2: One candidate (focus note) only