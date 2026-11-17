# Spelling objectives and cues: case validation

Case review: November 17, 2026, at `0f7994a0d6`.
This report retains minimal examples from the November 14 offline review;
the response rules below are informed adjudications, not a new blind review.

## Provenance and interpretation

The November 13 private export covers August 5–November 13. Its SHA-256 is
`3071dac175df47cd172c6bbc54f8fe887f475658f950ca340e9e441dde0ad3df`, matching
the November 14 replay manifest. Preparation observed the matching snapshot
on November 17 at `af84555ebf`; this review reused that snapshot and checked
the digest again. All ten selected attempts have recoverable cues. Each of
the five primary cues matches its manifest original; related attempts share
their family's cue. These are archived cues, not subsequent live note text.

The retained review used two independent AI reviewers, each answering 47
shuffled prompts across 24 selected cases, without keys, history, note titles
or the other's answers. This report reuses only the ten prompts for the five
families below. Packet identifiers locate retained judgments, not learners.
The reviewers saw both original and revised questions in their own packets;
independence is between reviewers, not between questions within a packet.

| Family | Original items | Revised items | Retained observation |
| --- | --- | --- | --- |
| Connections | A12 / B03 | A20 / B46 | Original: likely 伝手を頼る, with other expressions possible. Revised: both つて. |
| Honorific | A31 / B13 | A08 / B43 | Original: おっしゃる / 仰る, with restoration uncertainty. Revised: both おっしゃる. |
| Solution | A26 / B33 | A46 / B20 | Original: brining, brine and brined in different slots. Revised: both brine. |
| Inflection | A23 / B06 | A15 / B32 | Original: 死にかけ; one also proposed 死にかかっ. Revised: both 死にかけた; one explicitly allowed 死に掛けた. |
| Meadow | A22 / B38 | A14 / B21 | Both chose meadow in both versions; B38 and B21 retained grassland overlap. |

Private traceability is in `offline-replay/manifest.json`, `packet-a.md`,
`packet-b.md`, `reviewer-answers.md`, and `results.md` §2. The meadow guided
comparison is `second-pass.md` C07; its reported result is in `results.md`
“What needed another pass.” `final-wording-check.md` contains D01–D03 for
other families; it supplies no additional spelling judgment.

Historical correctness is a recorded outcome, not linguistic ground truth.
The five families were selected to expose problems, not sampled to estimate
error frequency. Agreement shows answerability for these reviewers, not a
unique key, deployed grading accuracy, learner comprehension or retention.

## Fixed objectives and response judgments

**A** means accepted under the declared rule; **R** means rejected under that
rule; **U** means unresolved and requiring review, not an automatic rejection.
Case and surrounding whitespace may vary in English responses; Japanese
script permission is stated separately. These are proposed human scoring
rules for comparison, not observations of a deployed grader.

### Culinary solution

Objective: retrieve the conventional culinary noun naming the salt-water
material used to submerge food. The rule requires the material's term with
correct spelling; a process or an inflected verb cannot substitute for it.
The revised cue supplies the material's composition, culinary setting and
one-word noun requirement; the learner must retrieve the lexical term.

| Response | Original mixed cloze | Revised solution objective |
| --- | --- | --- |
| brine / Brine | Fits the material slot, not every slot. | A: the solution noun; capitalization does not change spelling skill. |
| brining / brined | Each fits particular process/participle slots. | R: a process/verb form, not the requested material noun. |
| curing | Does not supply the solution or all the inflected slots; no whole-prompt credit established. | R: describes a preservation process, not the material. |
| brime | R as the spelling of this culinary term. | R: spelling error retained as a negative control. |

The actual old cue hides several different forms in one response task. The
rewrite is clearer by narrowing it to noun retrieval; that is a changed
target, not proof that the old rejection of curing was fair in every respect.
The dictionary distinguishes the noun and the verb's brined/brining forms.
[Merriam-Webster: brine](https://www.merriam-webster.com/dictionary/brine)

### Connections: phrase production versus reading

For whole-expression vocabulary production, the objective is to express
reliance on personal connections in a natural Japanese phrase. Script is
unrestricted; the rule credits meaning plus a natural construction, not
literal identity to one title. The original description supplies the meaning
but neither the precise expression nor a particle frame to practise.

| Response | Whole-expression objective under original cue | Revised reading-only objective |
| --- | --- | --- |
| つてをたよる / 伝手を頼る | A: same expression and particle; kana is permitted. | R: the whole expression exceeds the requested reading. |
| 伝手に頼る | Script passes; particle U for this exact collocation. It must not be rejected merely for using kanji. | R: not just the reading of the supplied word. |
| コネを使う | A for the broad meaning-production goal (both reviewers offered it); a particular lexical-expression goal needs a more specific cue. | R: a different expression, not the supplied word's reading. |
| つて | Incomplete as a response to a whole-expression goal. | A: correct hiragana reading of supplied 伝手. |
| でんて | R as the intended word's reading, not a script variant. | R: a reading error. |
| 自力で行う | R: contradicts reliance on other people's connections. | R: not the requested reading. |

Kanjipedia supplies the reading and a を頼る example. A research paper
notes に/を alternation with 頼る generally in its indexed introduction;
the full PDF returned HTTP 429 on November 17, so no fuller adjudication is
claimed. That general observation does not settle this exact collocation.
This study leaves 伝手に頼る's exact usage unresolved, rather than declaring
a particle error from title mismatch. Other public sources were read on
November 17; their definitions support these distinctions, not learner gains.
[Kanjipedia: 伝手](https://www.kanjipedia.jp/kotoba/0005087400),
[Akimoto: transitivity and particle alternation](https://repository.ninjal.ac.jp/record/3355/files/JC-G-08-02.pdf)

The revised sentence visibly supplies 伝手 and asks only for its hiragana
reading. The remaining retrieval is pronunciation, not the word, script or
whole construction. Both reviewers' つて answers establish answerability
of that different task; they do not validate original whole-phrase grading.

### Honorific written form

Objective: write the special lexical honorific corresponding to 言う in
hiragana, dictionary form. The rule requires that lexical form and its kana
sequence, including small っ. The revised cue supplies the base verb,
honorific category, script and dictionary-form instruction; lexical retrieval
and correct kana formation remain. The long original masks several forms
and explicitly permits kanji as well as kana, so its restoration task differs.

| Response | Original script-neutral interpretation | Revised hiragana objective |
| --- | --- | --- |
| おっしゃる | A as the lexical dictionary form. | A. |
| 仰る | A as the kanji form identified by both retained reviewers. | R for the requested script; not a claim of wrong Japanese. |
| おしゃる | R: omits small っ. | R: genuine form error remains distinguishable. |
| おっしゃった | Fits some original inflected contexts, not all masks. | R: past form instead of dictionary form. |
| 申す | R for a respectful honorific of the other person's speech. | R: different honorific role, not the requested special respectful form. |

The rewrite narrows form and script without making deletion of っ acceptable.
It tests lexical written-form recall, not full usage across all old examples.

### Complete ～かける inflection

Objective: produce one complete past-tense completion meaning “nearly died,”
using the supplied 死ぬ and ～かける. Script is unrestricted. The rule requires
the verb's connecting form, that construction and past ending, with no ending
duplicated outside the blank. The revised cue supplies the construction and
base verb; what remains is formation and inflection, not choosing the suffix.

| Response | Original blanks with た / ている already supplied | Revised complete blank before が |
| --- | --- | --- |
| 死にかけ / 死に掛け | A for the shared stem in the two supplied endings. | R: incomplete past-tense answer. |
| 死にかけた / 死に掛けた | Duplicates the old first ending if inserted verbatim. | A: complete requested formation; kanji allowed by this rule. |
| 死にそうだ / 死にそう | Cannot be transferred unchanged across both old endings; no whole-prompt credit established. | R: a different construction; near-death meaning alone does not satisfy the specified exercise. |
| 死ぬかけた | R: incorrect connecting form. | R: genuine formation error. |
| 死にかかっ | B06 proposed it as a meaningful alternative in the old slots; U as a target-specific answer. | R under the explicitly supplied ～かける rule; not a ruling that ～かかる is universally wrong. |

The retained review explicitly allowed the kanji completion; NINJAL lists
死にかける among compounds. The rewrite clarifies blank boundaries but also
changes unaided construction retrieval into guided formation. Its success
cannot establish the original task's fairness.
[NINJAL verb handbook](https://www2.ninjal.ac.jp/verbhandbook/headwords/%E6%8E%9B%E3%81%91%E3%82%8B%E3%83%BB%E6%87%B8%E3%81%91%E3%82%8B%E3%83%BB%E6%9E%B6%E3%81%91%E3%82%8B.html)

### Meadow and overlapping senses

Objective: retrieve a usual English term for the described grassy, flowered
open field. The rule permits defensible sense matches; demanding one hidden
name is not justified solely by the broad description. The original also
mentions hay and grazing. The revision foregrounds wildflowers and asks the
reader to deemphasize livestock, but still supplies no unique sense boundary.

| Response | Original field/hay/grazing description | Revised wildflower emphasis |
| --- | --- | --- |
| meadow | A: both reviewers' choice. | A: both reviewers' choice. |
| grassland | U for exact-name credit; B38 flagged a broader fit. | U: B21 still flagged the broader fit. |
| pasture | U: grazing is explicitly in the cue; automatic rejection is unsupported. | U: less aligned with its emphasis, but emphasis alone does not remove real-world overlap. |
| medow | R as the spelling of meadow. | R if producing that word; sense ambiguity does not license a typo. |
| forest | R: conflicts with the open, non-woody field. | R for the stated open-field sense. |

These U entries mark unresolved granularity under a single-name expectation,
not demonstrated language errors. Broader semantic credit could be defensible
under this objective; it cannot simultaneously establish precise-name recall.
Forage terminology places meadow within grassland and allows some grassland
within pastureland; NRCS defines pasture by grazing use and includes forbs.
[Forage and Grazing Terminology Committee](https://forages.oregonstate.edu/fi/topics/pasturesandgrazing/grazingsystemdesign/grazingterminology),
[USDA NRCS pasture](https://www.nrcs.usda.gov/resources/data-and-reports/pasture-resources)

The later C07 comparison supplies “hay meadow” and “pasture,” describes hay
cutting versus grazing, and asks about shared seasonal use. Its retained
result matches the primary uses and recognizes overlap. This is guided
classification with a word-overlap shortcut, not unaided vocabulary retrieval.
