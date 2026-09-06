---
id: SEED-010
status: dormant
planted: 2026-09-04
trigger_when: when execution efficiency is selected for improvement
scope: medium
consolidated: 2026-09-06
---

# SEED-010: Remaining execution-efficiency findings

P1 findings and their evidence now live in
[SEED-012](SEED-012-priority-execution-process-improvements.md).
This seed retains lower-priority proposals; none is an executable plan.

## Established baseline

Keep one coordinator-run selective formatter, fresh independent refactor review,
compact reusable proof, and destructive later-outcome protection. These are
already implemented. Original eight-slice measurements showed formatting-agent
setup consuming about 20% of subagent tokens for one mechanical change; refactor
review found three substantive duplication fixes. Preserve those improvements.

## Deferred proposals — P2

| Finding | Possible response | Evidence needed before adoption |
|---|---|---|
| Refactor prompts restated checklists and inherited full execution history; one-off research and broad diagnostics inflated coordinator context. | Pass current outcome, paths, constraints, and exact proof; isolate disposable research and return bounded diagnostics with provenance. Measure inherited context as well as prompt length. | Lower context/token cost without missed constraints or weaker independent review. |
| An untraced suspected exception-handling gap was delegated as confirmed; the implementer disproved it. | Phrase unverified claims as investigation questions; reserve confirmation for a traced call chain. | Delegations distinguish evidence from hypotheses without prescribing a false fix. |
| A concurrency-sensitive Vitest test exceeded the file-size guideline. | When applicable, extract per-concept `*.suite.ts` functions called by one thin runner, preserving single-file scheduling. | A matching case satisfies size guidance and preserves fixture isolation. |
| The final local-note slice exhausted fresh-agent capacity. Its first isolated refactor fallback emitted 21,478 truncated tokens without retaining the final handoff, so a second fresh review consumed another 26,549 tokens. | Preserve one fresh-review path through long executions. When an isolated CLI fallback is necessary, capture its final response through `--output-last-message` on the first invocation and bound streamed diagnostics; do not weaken independent review or the no-edit test skip. | The next capacity-exhausted review yields one attributable `REFACTOR COMPLETE` handoff with no repeated review and no lost proof. |
| Codex observer startup first failed at the sandbox/store bridge and required manual session recovery before the one usable observer was attached. | At the existing SEED-011 launch boundary, treat startup as established only after receipt directory/PID and a live notification bridge are available; otherwise report the bridge unavailable once. Reuse that exact identity through shutdown without replacement polling. | A later execution records one usable observer identity before its first push, needs no manual session reconstruction, and ends with an honest terminal receipt. |
| The local-note PLAN still said implementation had not started after all 14 slice statuses were done, and accumulated learnings grew it from 465 to 514 lines before cleanup. | Keep active PLAN lifecycle state single-sourced: derive or update the overall status at slice transitions, and prune spent diary/observer detail before the 500-line limit while retaining decisions and resume-critical evidence. | A later long PLAN remains status-consistent and at most 500 lines throughout execution, then receives normal completed-plan cleanup. |

## Related work and provenance

[Pygardon SEED-008](../../../pygardon/.planning/seeds/SEED-008-efficient-plan-execution-evidence-handoffs.md)
retains its local efficiency candidates. Broader CI recovery belongs to
[SEED-011](SEED-011-efficient-ci-failure-attention.md).

Source: notebook clone/publication retrospectives through `dd1ca6415a`, plus
the local-note creation execution `b876857bf2` through cleanup `d2d8fb9ff5`.
The detailed measurements, completed instruction trials, and retrospective
process trace remain in Git history.
