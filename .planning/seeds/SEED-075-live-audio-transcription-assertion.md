# Live audio transcription acceptance

<a id="story-1"></a>

### Accept either observed correct live audio transcription

**Identity:** SEED-075#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless"}
```

**Goal:** A developer can rely on the real-service live audio test when the
recording is transcribed as either "Please be quiet." or "請安靜".

**Scope:** Broaden only the real-service E2E transcription assertion to accept
these two phrases. Preserve its 20-second timeout and the assertion that the
existing note content survives. Product audio behavior stays unchanged.

**Examples:** Both phrases pass; unrelated text fails. The reported CI run
39869492105 at `3c228434` returned "請安靜" and failed the English-only assertion;
the following run passed with unchanged product code. The cause of the service's
language choice is unconfirmed.

**Execution:** The current contextual instruction authorizes this bounded test
repair without slice planning; no replanning, ten-minute implementation limit.
