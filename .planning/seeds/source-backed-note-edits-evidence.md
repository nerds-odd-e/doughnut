# Source-backed note edits: evidence

Evidence for the story
[Validate source-backed note edits from recall](SEED-073-learning-refinement-and-retrieval.md#validate-source-backed-note-edits).

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
