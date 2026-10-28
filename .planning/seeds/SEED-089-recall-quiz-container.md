# Read recall quizzes comfortably on wide screens

<a id="story-1"></a>

### Keep the entire recall quiz in a shared container

**Identity:** SEED-089#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless","assessment":"ready","reasons":[],"basis":{"document":"3cd68801cc22565e72ba6005de0c68c5a1b1f6b12264fbb2858bcdfce15ddc05"}}
```

**Goal:** Learners can read every recall question comfortably on a wide screen.

**Scope:** Move the existing centered maximum width from the multiple-choice question renderer to the recall page, covering spelling questions, answer feedback, and quiz controls. Keep the layout fluid on small screens.

**Known expectations:** Multiple-choice and spelling questions share the same width constraint. The quiz uses the existing `max-w-6xl` size, with automatic horizontal margins and full available width below the limit.

**Report:** Multiple-choice questions currently have their own container while spelling questions stretch across the screen. Source inspection at d14eefbfce confirms that `QuestionDisplay.vue` owns `max-w-6xl mx-auto`, while `RecallPage.vue` and `SpellingQuestionDisplay.vue` have no width constraint. The requested change is confined to presentation.

**Execution:** One bounded contextual layout change through dough-execute-plan, as authorized by dough-bug-fixing; no replanning. Run the existing frontend suite and both frontend typechecks. No new tests for this reversible styling change, following the session's developer instruction.
