# Source review: validate source-backed note edits from recall

Launch, 2026-11-18: a fresh general-purpose reviewer, no conversation
inheritance, given only the story section of
[SEED-073#validate-source-backed-note-edits](../../seeds/SEED-073-learning-refinement-and-retrieval.md#validate-source-backed-note-edits)
(Goal, Required behavior, Required restrictions, Out of scope, Key
examples, Limits, Effort hypothesis) with the source-only instructions. It
read no files and changed nothing.

Unnumbered context returned: the goal's reasons and "changes no product
behavior"; the out-of-scope list and deferred promises; the lessons
trial's four-week count as the reason for its restriction; ADR 0002 and
0003 as baseline; the effort hypothesis; the proof-method detail.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Six cases from the owner's own notes: the four faulty claims confirmed on 2026-11-18 (存じる tagged 自動詞; Solid OIDC as a specialization of WebID; the prisoner's dilemma motive; んじゃない as used only by men), one answer that fits only one sense of a word, and one attempt where the note is right ("Required behavior › Cases") | slice 2 | observed: .planning/seeds/source-backed-note-edits-evidence.md cases 1-6; private note-edits/cases.json holds 6 cases |
| 1a | The one-sense case is chosen from recurring mix-ups outside the lessons trial ("Cases") | slice 2 | observed: .planning/seeds/source-backed-note-edits-evidence.md case 5 (整う/調う, G16, outside both trial halves); cases.json slice2.trialCheck |
| 2 | A local review page shows, per case: the attempt (question, answer given, mark), the passage in the current note, the quoted source with its link, the smallest edit as a change against the current text, and the generator's question from the note before and after the edit ("Review page"; Key example 1) | slice 6 | |
| 3 | For the right-note case the page proposes "no change", quotes the source that supports the note, and the owner records whether he agrees or objects ("Cases"; Key example 3) | slice 6 | |
| 4 | Per case the owner records a verdict: accept as it is, accept with his own wording, or decline with a reason ("Verdict"; Key example 1) | slice 6 | |
| 5 | Per case and per addition (quoted source, smallest edit, question before and after), the owner records whether it changed or eased his decision ("Verdict"; Key example 1) | slice 6 | |
| 6 | Two real tries, one fault in a note property and one in the body, through today's note conversation in production; recorded per try: what was proposed, whether the rest of the note came back unchanged, whether he could judge it, minutes, decision ("Two real tries"; Key example 2) | slice 3 | observed: .planning/seeds/source-backed-note-edits-evidence.md 'Two real tries' table (tries made by the agent on the owner's delegation); proposals in ~/Downloads/solid-oidc-proposal.txt and prisoners-dilemma-proposal.txt |
| 7 | The product's generator runs locally on the note before and after each edit, and reviewers answer with the key hidden ("Question check"; Key example 5) | slice 5 | |
| 8 | Recorded per edit: whether the key of the question made after the edit agrees with the source ("Question check"; Key example 5) | slice 5 | |
| 9 | In the one-sense case, the edit accepts the answer for that sense (Key example 4) | slice 2 | observed: .planning/seeds/source-backed-note-edits-evidence.md case 5 edit; cases.json case totonou-one-sense edit accepts 調う for the 準備 sense, 整う only for 乱れのない状態 |
| 10 | Recorded: whether a valid near miss is still told apart after the edit; in the one-sense case the same answer in the other sense is still marked wrong after the edit ("Question check"; Key example 4) | slice 5 | |
| 11 | Per addition, a recommendation for the owner's review: design it, narrow and retest, or stop, with the counts and the owner's reasons ("Recommendation"; Goal) | slice 7 | |
| 12 | Evidence closeout follows the shared contract ("Recommendation") | slice 7 | |
| 13 | The result states its limits: one author, who is also the developer and was told the four faults on 2026-11-18; cases chosen, not sampled; generator output varies; sources found by an agent with web access, which today's assistant lacks ("Limits to state with the result") | slice 7 | |
| 14 | Production is read only, except an edit the owner himself accepts in the two real tries, through the existing acceptance and Git history ("Required restrictions") | slice 7 | |
| 15 | No attempt, grade or schedule is changed ("Required restrictions") | slice 7 | |
| 16 | Word pairs of the lessons trial are neither shown nor edited before 2026-12-16 ("Required restrictions"; Key example 6) | slice 7 | |
| 17 | A candidate pair that turns out to be in the lessons trial is named in the record (Key example 6) | slice 2 | observed: .planning/seeds/source-backed-note-edits-evidence.md 'Left out' paragraph: no candidate pair found in the lessons trial; 脇/腋 left out for lack of a source |
| 18 | Paid generator calls are sent only after the plan's cost, computed from real prices, has been shown to the owner ("Required restrictions") | slice 5 | |
| 19 | Attempts, note passages and the review page stay in the local cache and are not committed ("Required restrictions") | slice 7 | |
| 20 | The seed keeps only counts and a few short examples of this private evidence ("Required restrictions") | slice 7 | |
