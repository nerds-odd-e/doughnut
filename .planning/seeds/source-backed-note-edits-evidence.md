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
