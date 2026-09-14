# Git range comparisons and notebook identity

**Recommendation.** Treat a confirmed committed deletion followed by recreation
as a new Donut identity, including when both commits are published together.
Determine identity through the retained history, then project only the final
notebook. This is a product recommendation, not an approved ADR amendment.

The qualification matters: **Git does not prescribe this outcome.** Ordinary
endpoint diffs and pull-request summaries favor the net content result and can
hide recreation completely. IntelliJ's selected-commit change aggregation has a
different, directly relevant convention: it retains deletion and subsequent
addition as separate changes. Both conventions belong to ordinary Git tooling.
Choosing between them requires deciding what Donut identity means.

For a content-only viewer, the endpoint convention is a good default. For Donut,
where an identity carries learning history, conversations, and other private
data, preserving a known lifecycle interruption is more defensible than
silently assigning the previous identity to a replacement. The strongest
objection is equally concrete: an unpublished deletion followed by an ordinary
Git revert would also end the original identity. That cost needs to be accepted
as part of the policy; the research does not make it disappear.

**Scope and evidence.** Research checked on 2026-09-14. It covers official Git
documentation; ten Git applications or hosted review surfaces; IntelliJ and
Git Graph implementation evidence; ten controlled linear Git histories; and
one additional merge experiment. The application survey establishes available
comparison models, not market share. GUI edge cases are marked unknown where
documentation and source inspection do not establish them. The desktop clients
were not installed or exercised for this report.

**Donut's decision context.** The ADR index and in-file status agree that
[ADR 0002 — Git-native Portable notebook tree synchronization](adrs/0002-git-native-portable-notebook-synchronization.md)
is **Proposed**. Its current draft retains original history, validates and
projects only a publication tip, and leaves deletion gaps open. It already
distinguishes history analysis from replaying application operations. This
report supplies advice for that unresolved choice.

Accepted [ADR 0004 — OKF-compatible notebook Markdown profile](adrs/0004-okf-compatible-notebook-markdown-accepted.md)
keeps Donut IDs out of the Portable tree and explicitly leaves rename identity
outside the format decision. It also defines `_trash/` moves as preserving note
IDs and memory trackers. Accepted
[ADR 0001 — Ubiquitous language](adrs/0001-ubiquitous-language.md)
distinguishes a Portable path from stable Donut identity. These records do not
force either answer to the unpublished deletion-gap question. ADR 0001's
filename lacks the customary Accepted suffix, but its authoritative status
agrees with the index.

At inspected Donut commit `98eda027d776e16eb4830663ce9acc33de572174`,
`NotebookGitProposalAncestry.assertFollowsAcceptedHead` accepts an unchanged head
or one direct child, rather than arbitrary accumulated publication. The
controller test
`NotebookGitCopyIdentityControllerTest.recreatesSamePathWithAFreshIdentityAfterAcceptedPermanentRemoval`
specifies that **separately accepted** deletion and recreation produce a new
ID without the old private associations. The implementation calls permanent
removal for accepted deletion. These are repository observations, not a claim
that accumulated publication or the recommended policy is implemented.

There is no logical requirement to materialize B in order to use a fact about
B. A raw Git tree can establish that a tracked file disappeared even when
another file has malformed YAML. Conversely, a Markdown validation failure
does not establish that an identity died. **Final-only application validation
and history-sensitive identity are independent decisions.**

**Three different meanings of a combined view.** Consider linear history
`A → B → C → D`, with A already published.

| View | Question answered | Meaning for the example |
| --- | --- | --- |
| Endpoint comparison | How does the tree at D differ from A? | Compare A and D; intermediate deletion can disappear from the result. |
| Selected-change aggregation | What do the changes introduced by B, C, and D compose into? | Consume their changes in order; the aggregation may retain a deletion boundary. |
| History/provenance inspection | Which commits affected this path or these lines? | Expose additions, deletions, and inferred rename relationships over time. |

Selecting the commits B through D is not necessarily equivalent to comparing
B with D: the selected-change view can include B's change relative to A. A
comparison tool needs A and D to show the net result of all three unpublished
commits. Arbitrary nonconsecutive selections and merges introduce further
questions, but Donut's proposed linear range avoids those selection ambiguities.

Git's manual explicitly defines `git diff A D` as an endpoint comparison.
`A..D` in a diff is not an instruction to replay the range. A three-dot diff
uses the merge base as its first endpoint; on this linear history, with A an
ancestor of D, that is also A. Git's `--cc` combined format concerns merge
commits, not summarizing an ordinary linear sequence.[^1]

Likewise, `git range-diff` compares two versions of a patch series, pairing
commits between the series. It is useful when reviewing a revised series, but
is not the command for producing one net notebook change from A to D.[^2]

**What Git records and infers.** Commits reference trees and parents. Tree
entries associate names and modes with objects; blobs hold contents. Neither
structure supplies a persistent file-instance ID or a recorded rename command.
The same content can reuse the same blob after deletion, and two simultaneous
files can share that blob. Blob equality is content equality, not entity
identity.[^3]

Even `git mv` does not add an authoritative rename operation to a commit.
Git can recognize a rename when the author instead uses an ordinary filesystem
move followed by staging. This means the exact editing procedure cannot in
general be reconstructed from commits.[^4]

Rename detection transforms deletion/addition candidates into a pairing based
on compared contents. Copy detection examines additional possible sources.
Rewrite breaking with `-B` is also a content heuristic: it can analyze an
in-place rewrite as deletion/addition internally without proving a historical
deletion. None of these mechanisms records a durable lifetime boundary.[^5]

**Application comparison survey.** “Unknown” below means the specific
delete/recreate edge case was not established; it does not mean the feature is
absent. A documented endpoint comparison supports an inference about net
content, but does not establish every status label or rename setting in the
shipping GUI.

| Application or surface | Supported combined comparison | Recreation and rename evidence | Confidence and limits |
| --- | --- | --- | --- |
| IntelliJ IDEA: Compare Versions | Compare two arbitrary selected commits. | An endpoint view; its purpose differs from the selected-commit summary below. | Current official documentation. Exact GUI edge-case rendering not tested.[^6] |
| IntelliJ IDEA: selected-commit changed-files pane | Aggregates changes from multiple selected commits. | Source tests retain delete-then-add at the same path, cancel add-then-delete, and compose rename chains. A later rename of a re-added file remains on the addition side. | Strong source/test evidence; later-recreation-rename result is a trace of the implementation, not a GUI run.[^7][^8][^9] |
| GitKraken Desktop | Two selected commits show their difference; Shift-clicking multiple rows shows a combined diff. | Both interactions are expressly documented. No authoritative deletion-gap policy found for the multi-row aggregation. | Current official documentation; aggregation internals and recreation edge case unknown.[^10] |
| Tower, Mac | Compare two arbitrary revisions from History; supports branch comparisons too. | Supplies the requested overall comparison surface. The cited guide does not define lifecycle interruption handling or rename thresholds. | Official documentation; precise edge case unknown.[^11] |
| TortoiseGit | Select two revisions and invoke Compare revisions; inspect changed files and their diffs. | Supports a whole-repository comparison across commits, not merely one-file history. | Official documentation; recreation-specific labels not established.[^12] |
| SmartGit | Ctrl-select two commits for file lists and detailed differences. | File Log follows detected renames and offers optional copy-source investigation, explicitly treating copy origin as uncertain. | Official documentation distinguishes comparison from provenance inspection.[^13] |
| Git Extensions | Revision comparison includes two commits and multi-selection comparisons; settings also cover common-ancestor views. | Shows differences between selected revisions. These modes should not be mistaken for a durable file-lifetime model. | Version 5.0 documentation; recreation edge case unknown.[^14] |
| Git Graph extension for VS Code | Ctrl/Cmd-select two commits for a repository-wide comparison. | Inspected implementation invokes endpoint `git diff --find-renames`; consequently it has the endpoint information limit. | README plus pinned source. Inspected source revision is from 2021, not a freshly verified marketplace binary.[^15][^16] |
| VS Code built-in Source Control Graph | Compare a commit with a branch, remote branch, or merge base; multi-file diff display. | A related net-review surface. Documentation cited here does not establish arbitrary selected-commit aggregation. | Official documentation dated 2026-09-09; recreation edge case unknown.[^17] |
| GitHub Compare and pull-request Files changed | Commit comparison and an aggregate PR diff across the proposed changes. | PRs use a three-dot comparison. On a linear A-to-D range this compares the same endpoint trees; a hidden deletion gap cannot be recovered from those trees alone. | Explicit documented comparison semantics; no live recreation fixture posted.[^18][^19] |
| GitLab Compare revisions | Commit/branch/tag comparison with incoming-only or direct endpoint modes. | Documentation explicitly maps these to three-dot and direct Git diffs, respectively. Neither is a replay of intermediate deletions. | Explicit official command semantics; no live recreation fixture posted.[^20] |

IntelliJ is therefore **not unique in offering multi-commit summaries**.
GitKraken provides the especially close interaction of selecting multiple rows
for a combined diff. Many other tools offer two-revision comparisons spanning
an arbitrary number of commits. What is distinctive in the evidence collected
here is IntelliJ's explicit, tested treatment of deletion followed by addition
inside its change aggregation.

Fork and Sourcetree were also investigated. Their issue/community discussions
describe two-commit comparisons, but the located material did not establish a
maintainer-authored recreation policy. They are not counted as additional
confirmed lifecycle conventions. Expanding the list of similar comparison UIs
would not settle the missing identity contract.

**IntelliJ's relevant behavior in detail.** The inspected IntelliJ Community
revision is `cde5473c200d482652c6e492421ed93dbd008ced`, dated 2026-09-14. For a
multi-commit selection, `VcsLogAsyncChangesTreeModel` calls
`VcsLogUtil.collectChanges`. That collects each commit's changes in reverse
selection-list order and passes them to `CommittedChangesTreeBrowser.zipChanges`.
This establishes that the selected-commit pane consumes per-commit changes,
rather than simply requesting one endpoint diff.[^7]

`ZipChangesTest.testTricky` explicitly expects deletion of a path followed by
addition at that path to remain two changes. The reverse sequence—addition
followed by deletion—produces no final change. `testMovement` verifies chaining
renames and keeping a newly reused old path separate from the original file's
continued movement. These are expected change-list results, not Note IDs.[^8]

The implementation stores deletions separately while processing later changes.
A subsequent addition at the same path does not join the old deletion. A later
rename can join that addition, carrying it to the new path. Thus, tracing a
linear delete P; add P; rename P→Q input yields delete old P plus add Q. This
last compound case is an inference from the inspected implementation.[^9]

The aggregation method includes comments acknowledging imperfect collision
handling and the need for topology knowledge for arbitrary selections. Its
tests also describe some tricky results as open to improvement. It is useful
precedent for composing a linear history; it is not an identity oracle suitable
for transplanting wholesale into a database importer.[^8][^9]

**Controlled Git results.** Ten disposable repositories were constructed with
Git 2.50.1 (Apple Git-155). Global/system configuration was disabled, no remote
was configured, and hooks/signing were disabled in the fixtures. Each scenario
compared endpoints with `git diff --name-status -M50% A TIP` and inspected
adjacent commits with `git log --reverse --format=%s --name-status -M50% A..TIP`.
`-M100%` comparisons gave the same endpoint classifications for these fixtures.
The inputs are ordinary text blobs; they intentionally do not require legal
Donut notebooks, because these experiments concern Git evidence.

The original text has 100 lines, numbered 000–099, of
`Original notebook paragraph number NNN about stars and planets.` The complete
rewrite has 100 lines of
`Completely different replacement statement NNN: fungi, roots, and soil.`
Each line ends in a newline. These fixture strings distinguish the complete
rewrites from byte-identical restorations.

| Authored history after A | Observed endpoint result | Observed adjacent result |
| --- | --- | --- |
| B deletes Note; C recreates identical Note | No difference; equal tree IDs. | Delete Note; add Note. |
| B deletes Note; C recreates rewritten Note | Modify Note. | Delete Note; add Note. |
| B deletes Note; C recreates identical Note; D renames to New | `R100 Note → New`. | Delete Note; add Note; `R100 Note → New`. |
| B deletes Note; C recreates rewritten Note; D renames to New | Delete Note; add New. | Delete Note; add Note; `R100 Note → New`. |
| B renames Note→New unchanged; C completely rewrites New | Delete Note; add New. | `R100 Note → New`; modify New. |
| B moves Note→Other; C creates rewritten Note; D moves that to New | `R100 Note → Other`; add New. | Original move; independent addition; new file's move. |
| B creates Note, C deletes it; Keep remains throughout | No difference; equal tree IDs. | Add Note; delete Note. |
| B deletes Note; C is `git revert B` | No difference; equal tree IDs. | Delete Note; add Note. |
| B completely rewrites Note without a deletion commit | Modify Note. With `-B -M`, `M100`. | Modify Note. |
| Two identical files: actually move A→Y and B→X together | Git reports `R100 A → X`, `R100 B → Y`. | Same inferred pairings, contrary to the authored pairings. |

These examples establish three limits. First, perfect endpoint similarity does
not rule out a deletion gap. Second, a delete/add endpoint result does not rule
out an uninterrupted rename-and-edit history. Third, even adjacent exact-content
matching can choose the wrong original when duplicate files exist. `R100` is
not 100% confidence in an entity relationship.

File-history commands expose another distinction. In the same-path recreation
fixture, both ordinary path log and `git log --follow` included the recreation,
the earlier deletion, and the original addition. In the recreation-then-rename
fixture, `--follow` followed the rename and still included that older history.
**It did not stop at a durable “new file instance” boundary.** Git documents
`--follow` as a one-file history feature with limitations, not an entity-ID
resolver.[^21]

Default `git blame`, however, attributed all lines of the identical recreated
file to C. After its later rename, it still attributed them to C. The deletion
revert also attributed restored lines to C. A complete same-path rewrite
without a deletion gap attributed all lines to its rewrite commit too. Blame
can therefore expose fresh line provenance but cannot by itself decide whether
the enclosing Donut note is a replacement. Its documented purpose is line
attribution.[^22]

An additional experiment branched from A, edited Note on the other branch, and
merged the branch containing B's deletion and C's identical recreation. Git's
default `ort` strategy merged successfully and produced the same tree as the
edited branch. It did not replay a destructive deletion into that branch.
This agrees with Git's documented three-way merge treatment of net changes at
the heads and merge base. Donut v1 excludes merges; this experiment illustrates
Git convention, not a proposed expansion of Donut scope.[^23]

**Do these tools identify recreation, and do they care?** They care about
different observables. Endpoint views optimize for the final patch a reviewer
needs to understand. Omitting a temporary deletion can be exactly the correct
answer for that purpose. IntelliJ's aggregation retains additional change
structure. History and blame views answer provenance questions, and can expose
facts absent from the net patch.

SmartGit makes the uncertainty particularly visible: when file history reaches
an addition, its optional copy investigation searches the parent tree for a
similar possible source. Its documentation warns that this does not establish
actual copy origin. That is an example of tooling supporting investigation
without pretending the inferred relationship is an authoritative identity.[^13]

Language-aware tools add another layer. SemanticDiff documents recognizing
moved code and refactorings such as variable renames. Such analysis can make
changes easier to review, but the cited capability does not establish a
delete/recreate lifetime policy for a file, much less for Donut's private
learning data.[^24]

No surveyed source establishes a universal “recreated file” identifier that
survives subsequent moves. Some tools preserve enough information to distinguish
the events; that is different from proving whether a person meant replacement,
temporary removal, restoration, or a move split over commits. Identical recorded
trees can arise from different intentions.

**Policy alternatives for Donut.** The following are design choices, rather
than conclusions mandated by Git.

| Policy | Same-path delete/recreate within one unpublished batch | Principal advantage | Principal cost |
| --- | --- | --- | --- |
| Preserve across the unpublished gap | Keep the existing ID, subject to resolving competing lineages. | Matches net-diff expectations; a local delete/revert does not erase learning state. | Identical history can have different identity outcomes depending on publication boundaries; intentional replacements inherit old private data. |
| End identity at a confirmed committed gap | Recreated file starts a new ID; later moves carry that new lineage. | Composes with separately published deletion/recreation; retains a known historical interruption. | Even an unpublished delete/revert replaces identity; content-only comparison can conceal the consequence. |
| Refuse every deletion gap pending explicit intent | No acceptance until preserve/replace intent is supplied. | Avoids guessing either meaning for important private state. | Makes an ordinary Git editing sequence require a resolution mechanism Donut does not yet define. |
| Add an authoritative identity/operation channel | Let explicit identity or restore/replace intent settle the result. | Can express distinctions absent from snapshots. | Changes the protocol or portability constraints; does not follow automatically from ordinary Git metadata. |

Preservation is a serious alternative, not an incorrect reading of Git. If the
primary product promise is that local commits are merely drafts and publishing
means “make this final tree live,” it is arguably the more natural choice.
An accepted publication is a real application boundary, so different results
for a separately published deletion can be coherent under that model. Git's
endpoint and merge conventions provide substantial precedent for net-state
application.

The reason to prefer a new identity for Donut is a different product promise:
already supported changes should compose without silently changing who owns
their private data. A confirmed deletion terminates that relationship;
reintroducing bytes is a new relationship. This follows IntelliJ's relevant
aggregation distinction and today's separately published Donut behavior, but
neither source proves that it is every user's intended meaning.

**Recommended semantics.** Use content comparison for final content and a
history-derived correspondence for identity. In a permitted linear range,
carry the current accepted identities through supported same-path edits and
resolved moves. Allocate new identities only for new lineages that survive at
the final tip. Record ended existing lineages for final permanent removal.
Apply those final outcomes once, atomically.

An old path disappearing is insufficient to establish a death: its note may
have moved. Resolve supported movement first. If potential successors conflict,
or an edited move cannot be distinguished from deletion/addition under the
admission policy, keep the ambiguity explicit and refuse acceptance under the
Proposed ADR's conservative direction. Do not interpret failed rename detection
as proof of deletion.

A **confirmed gap** means the original lineage has no continuing representative
at a committed tree under the chosen admission rules. Reappearance after that
gap starts a new lineage even at the same path and with the same bytes. This is
the declared product meaning of a gap, not an inference that the author wanted
to discard learning history. If a later exact move is resolved, it carries the
new lineage forward.

| History | Recommended final identity result |
| --- | --- |
| Existing Note is edited repeatedly | Original ID with final content. |
| Existing Note moves to New, then New is edited | Original ID at New. |
| Existing Note is deleted, then recreated | Old ID ends; one new ID at Note. |
| Existing Note is deleted, recreated, then renamed to New | Old ID ends; one new ID at New. |
| Existing Note moves to Other; its old path is reused | Original ID at Other; new ID for the reused path. |
| A new unpublished note is added, moved, then removed | No final entity and no temporary private-data side effects. |
| Existing Note moves into `_trash/`, then out again | Preserve identity through resolved moves. |
| Markdown is invalid temporarily but repaired at tip | Invalid Markdown alone does not end identity; validate the final notebook. |
| Multiple identical notes make correspondence ambiguous | Do not trust a definite-looking Git pairing; refuse unresolved transfer. |

For histories whose intermediate tips are all separately admissible, this aims
for consistent surviving identity and private-data ownership whether the range
is published together or in several publications. It does **not** promise
identical generated ID numbers, timestamps, notifications, intermediate side
effects, or representability. Intermediate entities that never survive to the
tip need not be created at all.

For malformed intermediate drafts, analyze raw paths and blobs rather than
forcing a full notebook parse. A temporary non-Markdown pathname, a file-type
change, or movement across the notebook binding boundary needs an explicit
admission rule. Do not silently turn “not representable as a Note at B” into
“permanently deleted at B.” The initial policy can remain bounded to supported
transitions while rejecting cases it cannot resolve.

**The difficult consequence: deletion followed by undo.** Under the recommended
policy, a normal `git revert` of a committed permanent deletion restores
Portable content, but does not restore the previous private identity or learning
data. This is true even when deletion and revert arrive together and the final
tree equals the accepted tree. Automatically treating a commit message saying
“revert” or “restore” as special authority would be unreliable and introduce an
undeclared operation protocol.

By contrast, removing a file and putting it back before any commit leaves no
recorded gap. It is unchanged content, or a same-path edit if its contents
changed. Using Donut's portable trash offers the already defined reversible
removal model. If committed permanent deletion must also become reversible for
private data, that deserves an explicit retention/restoration design, rather
than resurrection that happens only when publication was delayed.

Because the recommended identity effect can be invisible in an endpoint diff,
the publication result must distinguish **replacement** from “no content
changes.” A preview or explicit consequence-bearing publish surface should make
the old identity's removal visible before it happens. This is a product-surface
requirement for adopting the recommendation, not an assertion that a suitable
preview or confirmation protocol currently exists. If that consequence would
remain hidden, do not ship automatic destructive recreation on the strength of
an empty Git diff.

**What the research changes in the decision.** There is strong evidence against
the claim that an overall Git rename proves uninterrupted identity. There is
also strong evidence against claiming that common Git applications uniformly
ignore reconstruction: IntelliJ explicitly preserves delete/add structure.
Neither fact requires application replay. Donut can choose history-sensitive
identity and final-only projection together.

The choice should therefore be stated as a Donut rule: **a committed permanent
deletion ends a resolved lineage; recreation starts another; publishing a batch
does not erase that boundary.** This is my preferred balance for private
identity ownership and composability. If protecting local undo from any
private-data loss takes priority, choose preservation across unpublished gaps
instead and document publication-boundary dependence as intentional. It is not
possible to obtain both properties under today's irreversible separately
published deletion semantics.

**Reproducing the central example.** Run this in a disposable directory. It
creates a local repository only; no Donut service or remote is involved. The
numbered content used in the broader experiment is unnecessary for the
identical-content case.

```sh
fixture_dir=$(mktemp -d)
cd "$fixture_dir"
export GIT_CONFIG_NOSYSTEM=1
export GIT_CONFIG_GLOBAL=/dev/null
git init -q -b main
git config user.name Fixture
git config user.email fixture@example.invalid
git config core.hooksPath /dev/null
git config commit.gpgsign false

printf 'Notebook content\n' > Note.md
git add Note.md
git commit -qm 'A: original'
git tag A

git rm Note.md
git commit -qm 'B: delete'
git tag B

printf 'Notebook content\n' > Note.md
git add Note.md
git commit -qm 'C: recreate'
git tag C

git diff --name-status -M50% A C
# No output.
git log --reverse --format=%s --name-status A..C
# B deletes Note.md; C adds Note.md.
git blame C -- Note.md
# Lines attributed to C.

git mv Note.md New.md
git commit -qm 'D: rename recreated file'
git tag D
git diff --name-status -M50% A D
# R100 Note.md New.md
git log --reverse --format=%s --name-status -M50% A..D
# Delete Note.md; add Note.md; R100 Note.md New.md.
git log --follow --format=%s --name-status D -- New.md
# Includes D, C, B, and A in this fixture.
```

The other linear scenarios are reproducible by substituting the two exact
100-line strings above and applying the operations in the experiment table.
Each B/C/D is a separate staged commit. For the duplicate case, start with
`A.md` and `B.md` containing identical original text and move them to `Y.md` and
`X.md`, respectively, before committing. For the merge experiment, branch from
A, append `Other branch addition` and a newline to Note.md, commit it, then
merge the branch at the identical recreation C. Git reports an `ort` merge and
the resulting tree equals the edited branch's tree.

**Evidence limits.** CLI observations apply to the specified Git version and
fixtures. They are not an accuracy benchmark, a JGit conformance test, or tests
of Donut's publisher. IntelliJ source and tests establish its core aggregation
behavior at the pinned revision; UI labels, plugins, filtered selections, and
other shipped versions can differ. Closed-source clients have not been assigned
undocumented recreation behavior. No source or experiment can reconstruct
intent that was not encoded in history.

**Sources.** Official pages were accessed on 2026-09-14; undated living
documentation is identified as such. Source links are pinned where the specific
implementation matters. Donut evidence is linked above and in the existing
[Git history as evidence for notebook identity](notebook-git-identity-research.md)
report.

[^1]: Git project. [git-diff](https://git-scm.com/docs/git-diff), living command manual; endpoint semantics, notation, and output formats.
[^2]: Git project. [git-range-diff](https://git-scm.com/docs/git-range-diff), living command manual; patch-series comparison.
[^3]: Scott Chacon and Ben Straub, Pro Git. [Git Objects](https://git-scm.com/book/en/v2/Git-Internals-Git-Objects), online second edition; blobs, trees, and commits.
[^4]: Scott Chacon and Ben Straub, Pro Git. [Recording Changes to the Repository](https://git-scm.com/book/en/v2/Git-Basics-Recording-Changes-to-the-Repository), “Moving Files,” online second edition.
[^5]: Git project. [gitdiffcore](https://git-scm.com/docs/gitdiffcore), living technical manual; rewrite, rename, and copy transformations.
[^6]: JetBrains. [Investigate changes in Git repository](https://www.jetbrains.com/help/idea/investigate-changes.html), IntelliJ IDEA 2026.2 Help; Compare Versions and file history.
[^7]: JetBrains. [VcsLogAsyncChangesTreeModel.kt](https://github.com/JetBrains/intellij-community/blob/cde5473c200d482652c6e492421ed93dbd008ced/platform/vcs-log/impl/src/com/intellij/vcs/log/ui/frame/VcsLogAsyncChangesTreeModel.kt#L188) and [VcsLogUtil.java](https://github.com/JetBrains/intellij-community/blob/cde5473c200d482652c6e492421ed93dbd008ced/platform/vcs-log/impl/src/com/intellij/vcs/log/util/VcsLogUtil.java#L272), source revision dated 2026-09-14; selection-to-aggregation call path.
[^8]: JetBrains. [ZipChangesTest.kt](https://github.com/JetBrains/intellij-community/blob/cde5473c200d482652c6e492421ed93dbd008ced/platform/vcs-impl/testSrc/com/intellij/openapi/vcs/changes/committed/ZipChangesTest.kt#L176), same revision; tests for cancellation, recreation, and movement.
[^9]: JetBrains. [CommittedChangesTreeBrowser.java, zipChanges](https://github.com/JetBrains/intellij-community/blob/cde5473c200d482652c6e492421ed93dbd008ced/platform/vcs-impl/src/com/intellij/openapi/vcs/changes/committed/CommittedChangesTreeBrowser.java#L315), same revision; aggregation implementation and collision caveats.
[^10]: GitKraken. [Access Diff, Blame, and History in GitKraken Desktop](https://help.gitkraken.com/gitkraken-desktop/diff/), living documentation; two-commit and multi-row combined diffs.
[^11]: Tower. [Comparing Branches & Revisions](https://www.git-tower.com/help/guides/commit-history/compare-branches-revisions/mac), living Mac documentation.
[^12]: TortoiseGit project. [Viewing Differences](https://tortoisegit.org/docs/tortoisegit/tgit-dug-diff.html), living manual; revision comparison dialog.
[^13]: syntevo. [Git Log](https://docs.syntevo.com/SmartGit/Latest/Manual/GUI/Log), latest manual; commit comparisons, following renames, and uncertain copy provenance.
[^14]: Git Extensions project. [Settings](https://git-extensions-documentation.readthedocs.io/en/release-5.0/settings.html), release 5.0 documentation; revision comparison and multi-selection behavior.
[^15]: Git Graph project. [README](https://github.com/mhutchie/vscode-git-graph), project documentation; two-commit comparison UI.
[^16]: Git Graph project. [dataSource.ts](https://github.com/mhutchie/vscode-git-graph/blob/d7f43f429a9e024e896bac9fc65fdc530935c812/src/dataSource.ts#L1775), revision dated 2021-09-19; getCommitComparison and execDiff.
[^17]: Microsoft. [View source control history](https://code.visualstudio.com/docs/sourcecontrol/history), dated 2026-09-09; built-in graph and history surfaces.
[^18]: GitHub. [Comparing commits](https://docs.github.com/en/pull-requests/how-tos/commit-changes/comparing-commits), living documentation.
[^19]: GitHub. [Branches: comparing branches in pull requests](https://docs.github.com/en/pull-requests/reference/branches#comparing-branches-in-pull-requests), living documentation; three-dot PR comparison.
[^20]: GitLab. [Compare revisions](https://docs.gitlab.com/user/project/repository/compare_revisions/), living documentation; explicit mapping of comparison modes to Git commands.
[^21]: Git project. [git-log](https://git-scm.com/docs/git-log), living command manual; path history and follow limitations.
[^22]: Git project. [git-blame](https://git-scm.com/docs/git-blame), living command manual; line attribution and optional movement/copy analysis.
[^23]: Git project. [git-merge](https://git-scm.com/docs/git-merge), “Merge strategies,” living command manual; default ort and treatment of reverted changes in three-way merging.
[^24]: SemanticDiff. [SemanticDiff for VS Code](https://semanticdiff.com/vscode/), living product documentation; language-aware diff and moved-code/refactoring detection.
