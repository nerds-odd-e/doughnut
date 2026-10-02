# Numbered-property startup migration

The temporary `NumberedPropertyStartupMigration` runs synchronously on each
non-test `ApplicationReadyEvent`, immediately after
`FlyWayFreeVersionRealMigration` finishes `repair()` and `migrate()`. It uses the
injected production `NumberedPropertyMigration` runner. There is no activation
flag, Flyway placeholder, schema change or public migration endpoint.

The runner visits notebooks in ID order and derives eligibility from current
stored Markdown, including Trash. Exact authored list-capable property families
with numeric suffixes of at least 2 become one list, including `url` and aliases.
Base values precede suffixes in numeric order; source-list order is retained and
the first equal value wins. Case-distinct authored families, scalar structural
properties and word suffixes remain distinct. Unrelated content is preserved.

Trackers follow their original scalar value or list item for every learner and
persisted state. For duplicate database destinations, the existing destination
tracker wins; otherwise the lowest ID wins. Redundant trackers are deleted by the
normal deletion owner, including cascaded recall logs, prompts and batch requests
and the conversation prompt SET NULL edge. The survivor keeps its own schedule
and history; histories are not merged.

Authored property selectors are retargeted only when their resolution can be
preserved for every current reader, including anonymous readers of public
sources. Alias candidate changes are included. Unrepresentable content, orphan
learning focuses and inconsistent reader resolutions produce a notebook
diagnostic in startup logs. The complete affected operation remains unchanged;
a reported refusal is unfinished migration work.

Each operation rechecks current content under ordered binding locks and commits
independently through the existing accepted-change service. All changed bound
notebooks receive one Donut System Git descendant atomically with their content,
derived state and learning changes. Learning remains private and is absent from
portable Git content. Unbound notebooks use the existing persistence path.

Unexpected infrastructure failures surface loudly and stop the run. Earlier
operations remain committed. A later startup retries remaining legacy content;
completed operations keep their accepted heads and learning identities. Repeated
ready events and concurrent instances use the same transaction and lock boundary.

Before removing the temporary code, confirm operationally that eligible legacy
content is absent and no outstanding migration diagnostics remain. The queued
cleanup story `SEED-063#story-3` depends on that confirmation. Passing isolated
fixtures proves behavior, not completion in a deployed database. Deployment and
release remain separately authorized; this guide does not authorize either.
