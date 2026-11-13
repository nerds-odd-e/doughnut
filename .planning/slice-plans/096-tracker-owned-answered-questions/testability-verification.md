# Testability domain seams

Slice 4 extracts the original 466-line facade into cohesive existing-pattern
mixins: note injection/IDs/content/rename, relationship composition/seeding,
and conversation setup/replies. The facade is 211 lines; new homes are
153/97/44 lines. Method bodies, aliases, request values, chaining and callers
remain intact; added contracts are type-only. Reset shares the note owner's
alias. Question helpers remain for slice 6. Root inspected all four files
before the expensive run and confirmed the planned seams.

Literal proof command:

```sh
CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/messages/message_center_with_unread_message_count.feature,e2e_test/features/relationships/relationship_edit_and_remove.feature,e2e_test/features/recall/recall_quiz_ai_question.feature,e2e_test/features/note_creation_and_update/note_creation.feature,e2e_test/features/note_creation_and_update/note_edit.feature' > /tmp/donut-slice4-e2e.log 2>&1
```

Terminal exit 0: 33/33 scenarios passed, zero failures/pending/skips,
Cypress duration 3m59s. Inputs and assertions stayed fixed through completion.
The wrapper owned and ended its isolated stack/private mock invocation;
origin `http://127.0.0.1:60576`, profile `e2e`, database
`doughnut_e2e_wt_6c52c877a9c84ab186ec2c44fc117692`.

| Selected feature | Scenarios | Preserved setup and observing outcome |
| --- | ---: | --- |
| Message-center unread counts | 4 | Conversation IDs/start/replies; receiver and circle counts, reading clears count. |
| Relationship edit/remove | 5 | Note/relationship injection and content updates; suffix/frontmatter, deletion and resulting property tracker. |
| Recall quiz | 1 | Injected note ID lookup; generated question and incorrect selected answer. |
| Note creation | 9 | Seeded structure and remembered UI-created IDs; created note/folder/sidebar outcomes. |
| Note editing | 14 | Content injection and rename-map lookup; edited/renamed note, undo and persistence. |

Consumer trace: `/tmp/donut-slice4-consumer-trace.md`, all 129 rows
dispositioned. Caller/step wording leads to the five selected feature purposes;
unselected CLI/MCP/publication journeys are not claimed run. Relocated setup
purposes are mapped to sufficient selected observations. No distinct missing
API/alias/this-binding purpose was found. Owning features select page objects,
not MakeMe; existing focused objects and scenario inputs were preserved.

Prelaunch Mac decision: 16 logical CPUs/48 GiB RAM, load
20.35/23.24/28.81, memory pressure 41% free, no active heavy worker. One batch
was chosen, with reconsideration after failure/contention; it passed without
retry. Editing/inspection took about five minutes; stack/proof waits were
separate. No production, SDK, feature or assertion changed, and no generator
is triggered. Source freshness was unchanged at `9baf479dda`; no named sibling
coordination applies.

Independent refactoring gave the relationship note-title/relation defaults
one private home and removed an unreachable empty-title guard. Only that
module changed (94 lines); all 23 facade methods, aliases, requests and
dynamic dispatch were independently compared. Renewed command:
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/relationships/relationship_edit_and_remove.feature > /tmp/donut-slice4-refactor-e2e.log 2>&1`.
Exit 0: five scenarios passed, no failures/skips, 1m11s. The other 28 accepted
scenarios' code/setup/observations remain unchanged and were reused. Trace:
`/tmp/donut-slice4-refactor-consumer-trace.md` (130 dispositions).
Refactor marker and whitespace passed; all changed files are under 250 lines.
