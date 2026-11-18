# Source-backed note edits: evidence

Evidence from validating source-backed note edits after recall misses
(SEED-073, 2026-11-18). The follow-ups are
[showing the smallest change](SEED-073-learning-refinement-and-retrieval.md#source-and-smallest-change-in-corrections)
and [quoting a source the assistant found](SEED-073-learning-refinement-and-retrieval.md#assistant-quoted-source-in-corrections).

## Fault cases (2026-11-18)

The four notes were read from production on 2026-11-18. Each faulty
claim was still in the note. Each case has a source that was opened and
quoted, and the smallest edit. Four cases kept, none dropped.

1. **存じる** (fault in a property). Claim: the note is tagged
   自動詞; one missed question keyed 自動詞 when 他動詞 was chosen.
   Source: 精選版 日本国語大辞典 on
   [kotobank](https://kotobank.jp/word/%E5%AD%98%E3%81%98%E3%82%8B-555898)
   gives 「他動詞 ザ上一」; a noun-object example, 「その作者を存じて居りません」
   (太宰治), is on [Wiktionary](https://ja.wiktionary.org/wiki/%E5%AD%98%E3%81%98%E3%82%8B).
   Edit: tag 自動詞 → 他動詞, and add that example to the body. Limit:
   Wiktionary labels the 思う sense 自動詞.
2. **Solid OIDC** (fault in a property). Claim: "a specialization of:
   WebID"; one missed question keyed WebID when OpenID Connect was
   chosen. Source: the [Solid-OIDC specification](https://solid.github.io/solid-oidc/):
   it "builds on top of OpenID Connect 1.0", and WebIDs are "a primary
   identifier for Users". Edit: remove the property; add "It uses WebID
   as the primary identifier for users." to the body.
3. **prisoner's dilemma** (fault in the body). Claim: each confesses "to
   prevent their counterparts from receiving a lesser punishment".
   Source: [Stanford Encyclopedia of Philosophy](https://plato.stanford.edu/entries/prisoner-dilemma/):
   "whatever the other does, each is better off confessing". Edit: the
   motive becomes "because, whatever the other does, each is better off
   confessing".
4. **〜んじゃない(か) vs 〜じゃない(か)** (fault in the body). Claim: the
   summary says the form with ん is men's language. Source: the
   [article the note cites](http://www.edewakaru.com/archives/5419032.html)
   says the form becomes men's language when it ends in か. Edit: the
   summary sentence becomes 「また、最後に「か」がつくと男言葉になります。」
   Limit: no independent grammar reference on gender use was found; the
   missed question was about meaning, not gender.

## One-sense and no-change cases (2026-11-18)

Both notes were read from production on 2026-11-18. Neither is a
lessons-trial pair or one of the four fault notes. Six cases now exist.

5. **整う** (typed word fits one sense of the cue only; group G16,
   outside both trial halves). Claim: two spelling misses typed 調う for
   the 整う cue. The note's 準備が完了する sense (必要不可欠なものが不足なく揃う)
   is the sense 調う also covers. Source: the 文化庁
   [「異字同訓」の漢字の使い分け例](https://www.bunka.go.jp/seisaku/bunkashingikai/kokugo/hokoku/pdf/ijidokun_140221.pdf)
   gives 「【整う・整える】乱れがない状態になる。」 and 「【調う・調える】必要なものがそろう。」
   Edit: the 準備が完了する bullet adds that this sense may also be written
   調う, and that the 乱れのない状態 sense (「列が整う」) is 整う only.
   Limit: the report's examples do not name 準備が整う itself.
6. **〜ことだから confused with 〜ことから** (no change). Claim: one missed
   question (row 4032) chose ことから／ことだから; the 2026-11-14 replay
   kept the question as a sound control. Sources: the
   [edewakaru 〜ことだから article](https://www.edewakaru.com/archives/12106149.html)
   (judging from a known person's character or habits) and
   [JLPT Sensei ことから](https://jlptsensei.com/learn-japanese-grammar/%E3%81%93%E3%81%A8%E3%81%8B%E3%82%89-koto-kara-meaning/)
   ("from the fact that ~") support the note's split. Proposal: no change.

Left out: 脇/腋 (G30, outside both trial halves) was not chosen; 整う/調う
had an opened source showing the one-sense fit, and no source was opened
for 脇/腋. No candidate pair was found in the lessons trial.

## Two real tries of today's note conversation (2026-11-18)

The owner asked the agent to make both tries and the decisions on his
behalf. So the tries were made by the agent, which already knew the
fault and the source, through the production page "Send message and
invite AI to reply". Minutes are the agent's, not a person's.

| Note | Fault in | What was asked | What was proposed | Rest of the note | Could judge | Minutes | Decision |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Solid OIDC | property | not a specialization of WebID; it builds on OpenID Connect and uses a WebID | `a specialization of: WebID` → `uses: WebID`; also `an instance of: OpenID Connect` → `builds on: OpenID Connect` (not asked); body rewritten to say it builds on OpenID Connect, layered on OAuth 2.0, and uses a WebID | `type`, `url` and `a part of` kept; one property not asked about changed; body sentence replaced | yes: whole content shown, change found by comparing both versions | about 1.5 | Accept (revision 5ed4e72e) |
| prisoner's dilemma | body | the motive is wrong; confessing is better whatever the other does | the faulty sentence replaced by "confessing gives them a lighter sentence than staying silent, regardless of what the other prisoner does", plus one sentence naming the strictly dominant strategy | properties and the other sentences unchanged | yes | about 1 | Accept (revision fa5efa4d) |

Both proposals came on the first request, each with a short reason, and
both agree with the sources in the fault cases above. A read of each
note through the API after acceptance showed the new text.

## Generation requests and cost (2026-11-18)

A scratch backend test, since removed, wrote the product's question
generation request for each case from its frozen note content: 11
bodies (before and after for five cases, before only for the no-change
case), each with model `gpt-6.2-sol` and an output limit of 1,000 tokens.
Linked notes were created with their current content; production also
adds second-level links, folder siblings and back links, which the local
request lacks.

- Input size: about 1,190 to 2,150 tokens a body (estimated: ASCII at
  four characters a token, other characters one each).
- Price: $2.00 per 1M input and $10.00 per 1M output tokens, from
  [OpenAI's pricing page](https://developers.openai.com/api/docs/pricing),
  read 2026-11-18.
- 33 calls (three per body): about $0.43 in total, at most about $0.014 a
  call. The owner answered go on 2026-11-18.

## Question check (2026-11-18)

33 paid calls to `gpt-6.2-sol` after the owner's go: all returned HTTP 200
on the first try; 45,846 input and 8,464 output tokens, about $0.18. Two
reviewers in the agent's session answered all 33 questions from a
shuffled packet that held only stem and choices.

| Case | State | Reviewers on key (A, B) | Key agrees with source | Questions on the edited claim | Near miss told apart |
| --- | --- | --- | --- | --- | --- |
| 存じる | before | 3, 3 | 3 of 3 | 0: all ask which verb 存じる humbles | — |
| 存じる | after | 3, 3 | 3 of 3 | 0: the new example appears, but no question on 他動詞 | not tested |
| Solid OIDC | before | 3, 3 | 3 of 3 | 0: all ask its role | — |
| Solid OIDC | after | 3, 3 | 3 of 3 | 0: all ask its role | not tested |
| prisoner's dilemma | before | 3, 3 | 3 of 3 | 0: all ask the outcome; the faulty motive is never keyed | — |
| prisoner's dilemma | after | 3, 3 | 3 of 3 | 2: "better off confessing no matter what the other does" | yes: "better only if the other confesses" is a distractor in both |
| んじゃない | before | 3, 3 | 3 of 3 | 0: all ask meaning by ん and intonation | — |
| んじゃない | after | 3, 3 | 3 of 3 | 0: same, gender never asked | not tested |
| 整う | before | 3, 3 | 3 of 3 | 0: all ask the 準備 sense | — |
| 整う | after | 3, 3 | 3 of 3 | 2: which sense may be written 調う; how to write 「列がととのった」 | yes: 調う is a wrong choice for the 列 sense, and the 列 sense is a wrong choice for 調う |
| ことだから (no change) | before | 3, 3 | 3 of 3 | — | — |

Neither reviewer flagged a question. The weakest key is one 整う after
question that gives both senses' shared meaning as "その場にふさわしい状態になる",
a loose fit to the 文化庁 split. Reading: no before question asked about a
faulty claim, so these faults were not reaching questions; after the edit,
the generator asked about the change only where the edit put it in the
body's main sentence (two of five cases), never about a property.

## The owner's verdicts (2026-11-18)

The owner filled in the local review page himself and saved his answers
at 03:58Z. He said he was not quite sure of them and welcomed advice.

| Case | Verdict | Quoted source | Smallest change | Before-and-after question |
| --- | --- | --- | --- | --- |
| 存じる | accept as it is | eased | changed | eased |
| んじゃない | accept as it is (corrected, see below) | changed | changed | eased |
| 整う | decline: "The focus was in 整う, I assume. But the question is about 調う" | eased | no effect | no effect |
| ことだから | agree: no change | eased | — | eased |
| Solid OIDC (already accepted) | — | changed | changed | no effect |
| prisoner's dilemma (already accepted) | — | changed | changed | no effect |

Per addition, cases where it changed or eased the decision: quoted
source 6 of 6 (changed 3, eased 3); smallest change 4 of 5 (changed 4);
before-and-after question 3 of 6 (eased 3, changed none).

Two answers need reading:

- んじゃない: he saved "accept with my wording", but the wording he gave
  is not wording for the note: "the 2nd question used the wrong language
  (English)". The after question shown (run 1 of 3) was in English for a
  Japanese note. On the agent's advice he agreed on 2026-11-18 to record
  it as accept as it is, plus a fault found in the generated question.
- 整う: the decline says the edit taught 調う inside a note about 整う.
  The case came from spelling misses (調う typed for the 整う cue), and
  the edit answered them by adding the other spelling's sense to this
  note, which moved the note's focus.

## Recommendation (2026-11-18)

For the owner's review; he agreed with it on 2026-11-18. No pass mark
was fixed in advance.

| Addition | Call | Behind it |
| --- | --- | --- |
| Quoted outside source | design it | changed or eased the decision in 6 of 6 cases (changed 3) |
| Edit shown as the smallest change | design it | changed the decision in 4 of 5 cases; the 整う decline came from what the edit said, not from how it was shown |
| Question from the note before and after the edit | narrow and retest | eased 3 of 6 decisions and changed none; questions reached the edit in 2 of 5 after cases and never in a property; one of the shown questions came in English for a Japanese note |

What to narrow before a retest: aim the question at the changed passage,
including a changed property, and keep the note's language.

Compared with today's conversation (two tries): both proposals came on
the first request with a short reason, and both were right. What today's
review lacks is what the page added: the quote and the change shown
alone. In the Solid OIDC try one property not asked about also changed,
which a full-content review makes easy to miss; the owner marked the
smallest change as changing his decision on both tried cases.

Other findings:

- No question made before an edit asked about the faulty claim (0 of
  15): these faults were not reaching the questions.
- The 整う case came from spelling misses. The edit answered them by
  teaching 調う inside the 整う note, and the owner declined it. Help for
  spelling attempts stays out of scope.
- Row 8502's stored answer key for the prisoner's dilemma was itself
  wrong.

Limits:

- One author, who is also the developer and was told the four faults
  during refinement on 2026-11-18. He delegated the two conversation
  tries and their decisions to the agent, which already knew the faults
  and sources; their minutes are the agent's.
- Cases were chosen, not sampled: nothing here says how often a recall
  miss comes from a faulty note.
- Generator output varies between runs: three questions a state, and the
  page showed one.
- Sources were found and quoted by an agent with web access, which
  today's assistant does not have. Two sources (存じる, んじゃない) are weak.
- The local request lacked production's wider context, and token counts
  before sending were estimates.

## Assistant search try (2026-11-18)

After the owner's go on 2026-11-18 (limit $4), 12 paid requests went to
`gpt-6.2-sol`, the production model for note conversations: two for each
of the six cases above. Each was a streamed request of the kind the note
conversation sends, with the web search tool added (at most 5 tool calls)
and two new optional parts on the content correction tool: a quote and
the address of its page. The request asked for a search before a
correction, the note's own `url` first, and a passage copied word for
word. The note content was as read from production on 2026-11-18; for
Solid OIDC and the prisoner's dilemma it was the content before
revisions 5ed4e72e and fa5efa4d. The author's message stated the fault,
as in the two real tries. The scratch script is deleted.

| Case | Run | Search steps | Input / output tokens | Cost | Source | Quote on the page |
| --- | --- | --- | --- | --- | --- | --- |
| 存じる | 1 | search | 8,543 / 304 | $0.030 | kotobank | yes |
| 存じる | 2 | search, open page | 10,201 / 333 | $0.024 | kotobank | yes |
| Solid OIDC | 1 | open page, search | 13,033 / 305 | $0.039 | Solid-OIDC specification (the note's `url`) | yes |
| Solid OIDC | 2 | open page, search | 13,125 / 306 | $0.030 | same | two passages joined with "…"; each is on the page |
| prisoner's dilemma | 1 | search, find in page | 12,921 / 316 | $0.039 | Cornell book chapter (PDF) | yes |
| prisoner's dilemma | 2 | search | 8,913 / 293 | $0.021 | Stanford Encyclopedia of Philosophy | yes |
| んじゃない | 1 | open page, search, find in page, open page twice | 31,761 / 1,404 | $0.088 | Kobe Gakuin paper (PDF) | yes |
| んじゃない | 2 | open page, search, find in page | 22,531 / 1,211 | $0.057 | Osaka University paper (scanned PDF) | yes to the eye; the PDF's text layer reads ウイーク where the quote has ウィーク |
| 整う | 1 | search, open page | 11,563 / 746 | $0.041 | 教育出版 | yes |
| 整う | 2 | search twice, open page | 14,241 / 728 | $0.046 | 国立国語研究所 | yes |
| ことだから (no change) | 1 | search, find in page, search | 13,292 / 295 | $0.050 | — | no correction proposed |
| ことだから (no change) | 2 | search, find in page, search | 13,423 / 330 | $0.041 | — | no correction proposed |

- All 12 completed, in 7 to 25 seconds. Search, the correction tools and
  streaming worked in one request.
- All 10 corrections carried a quote and an address. No quote was
  invented: 8 are on their page word for word (6 web pages, 2 PDFs), one
  joined two true passages, and one differs from a scanned PDF's text
  layer by one small character.
- 4 of the 10 sources were PDFs.
- The two んじゃない quotes are example sentences spoken by women. They
  support the correction only indirectly. The note's own `url` holds the
  direct sentence; the assistant opened that page first and quoted
  another source.
- The no-change case proposed nothing in both runs, and still searched.
- Solid OIDC run 2 again changed `an instance of` to `builds on`, which
  was not asked.
- Cost: $0.51 in total, counting $0.01 for each search, input at $2.00
  and output at $10.00 per 1M tokens. If opening a page and finding in a
  page are also charged as searches, $0.66. A correction cost $0.02 to
  $0.09.

Limits: the request held the note alone, without the linked notes that
production adds. Two runs a case; output varies between runs. The author's
message named the fault. Whether a quote supports the claim was read by
the agent, not by the owner.

## Closeout (2026-11-18)

This story's private folder `note-edits/` was deleted after these
conclusions were written, together with the proposal and answer copies in
`~/Downloads` (moved to the Trash). No approved follow-up needs that
evidence. The shared cache `.planning/tmp/recall-history/` stays: other
SEED-073 validations are still queued, and the lessons trial runs to
2026-12-16. The two accepted note corrections stay in the product.
