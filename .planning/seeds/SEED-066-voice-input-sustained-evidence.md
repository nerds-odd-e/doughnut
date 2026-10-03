# Voice-input sustained-speech evidence

**Source:** [SEED-066 discovery story](SEED-066-voice-input.md#discover-voice-input-problems).
**Capture setup:** [Short-speech baseline](SEED-066-voice-input-manual-evidence.md#recording-route-and-short-speech-baseline).

## Sustained speech and revision evidence

**2026-10-03, 01:47–01:51 UTC:** Chrome on macOS, same signed-in `manual`
localhost Development session, disposable notebook `23`, note `13726`. Running
backend revision remains the preparation observation `a02dbb2697…`; this slice
did not re-read it. The checkout's evidence base is
`2eec224d133f9bfa081e5918a55c46f4de3827a1`. Services were reused, with real
transcription/retouch and persistence; no services were restarted or mocked.

**Expectation and discrepancy:** The owner's content-instability report asks
to preserve completed content while allowing unfinished-sentence revision.
The exact acceptable recent-sentence boundary remains undecided. A first
intermediate result contained two completed orchard sentences and an unfinished
book clause. Flush replaced all of that new paragraph with a middle fragment;
the next automatic result replaced the fragment with the final three sentences.
Reload confirmed that the orchard facts and book clause were lost. The entire
preexisting six-sentence Harvard paragraph from the baseline survived unchanged
at every snapshot; this observation is loss of completed content in the current
recording, rather than whole-note erasure. No cause was investigated.

**Reproduction and input:** Navigate via Note → notebook “Voice discovery
20261003 baseline” → note `13726` (`/n13726`), open Audio tools, install the
baseline report's temporary capture harness with the fixture URL and device
label changed to the file below and `Controlled sustained speech`, then Record
Audio. Generate the owned fixture in the execution checkout with:

```bash
/usr/bin/say --file-format=WAVE --data-format=LEI16@48000 -r 150 -o .voice-discovery-long.wav 'The orchard contains apple trees, peach trees, and a small wooden bench. These facts are finished. The book that I bought yesterday, [[slnc 8000]] after reading several reviews and comparing different editions, is a gift for my sister because she enjoys learning about the history of gardens. The meeting is on Friday afternoon. We should bring a notebook and a pencil.'
```

The exact harness fetch URL was
`/@fs/Users/terryyin/git/doughnut/.worktrees/discover-voice-input-problems-through-manual-tes/.voice-discovery-long.wav`.
Decoded duration was 29.168 s, mono, 48 kHz. A Python `wave` read of that
fixture confirmed 1,400,064 frames; contiguous 0.1-second sample blocks with
maximum absolute 16-bit amplitude below 100 identified the intended long pause
at 8.4–16.6 s. Playback was naturally paced through AudioContext →
MediaStreamDestination, with the real worklet. Speech was synthesized by the
installed macOS `say` command. Hardware capture, permission and device selection
remain uncovered. Neither clocks nor responses were mocked.

For body/title timing, the page harness additionally observed `#app` with a
MutationObserver (`subtree`, `childList`, `characterData`), recording
`new Date().toISOString()`, `.ql-editor.innerText` and
`[role="title"].innerText` only when body or title changed. CDP `Network.enable`
and `readEvents` observed request/response/loading events. Activate Flush about
22 s into capture, continue to the end of the source, then Stop. The actual
Flush click occurred about 01:49:09.44 UTC (marker written after the state
read at 01:49:09.757); Stop occurred about 01:49:31.9 (marker 01:49:32.048).

| Stage (UTC) | Elapsed from capture | Evidence |
| --- | --- | --- |
| Capture starts 01:48:47.385 | 0 s | Original Harvard body and title “Sensory Impressions of Foods and Drinks” |
| First audio request 01:48:58.772 → response 01:49:04.018 | 11.39 → 16.63 s | HTTP 200, 5.25 s request |
| First body 01:49:04.111 | 16.73 s | Content PATCH completed 01:49:04.100, appended paragraph A below |
| First title 01:49:06.230 | 18.85 s | “Sensory Impressions of Food and Drink Experiences” |
| Flush request 01:49:09.442 → response 01:49:12.585 | 22.06 → 25.20 s | HTTP 200, 3.14 s request |
| Flush body 01:49:12.684 | 25.30 s | Content PATCH completed 01:49:12.671; A replaced by B, ~3.24 s after Flush |
| Next title 01:49:14.476 | 27.09 s | “Sensory Impressions of Food and Drink” |
| Source playback ends 01:49:16.553 | 29.168 s | Natural source end |
| Final automatic request 01:49:19.537 → response 01:49:23.069 | 32.15 → 35.68 s | HTTP 200, 3.53 s request |
| Final body 01:49:23.138 | 35.75 s | Content PATCH completed 01:49:23.129; B replaced by C, 6.59 s after source end |
| Stop approximately 01:49:31.9 | ~44.5 s | Already-settled body; Stop disabled and Record enabled afterward |

These timings distinguish recording, requests, persistence and visible text.
The final body had settled before Stop, so this run provides no post-Stop
processing duration. All observed content/title PATCH responses were HTTP 200.

**Intermediate and final text:** Each body retained the exact Harvard paragraph
quoted in the baseline, followed by a blank line and just the following
paragraph. The title varied separately as shown above.

- **A:** “The orchard contains apple trees, peach trees, and a small wooden bench.
  These facts are finished. The book that I bought yesterday.”
- **B:** “After reading several reviews and comparing different editions, is a
  gift from my sister because she”
- **C (final saved):** “She enjoys learning about the history of gardens. The
  meeting is on Friday afternoon. We should bring a notebook and a pencil.”

Reload at 01:49:48 confirmed original Harvard paragraph plus C and title
“Sensory Impressions of Food and Drink”. A tool screenshot captured the saved
note after reload; no standalone screenshot file was created. In addition to
erasure, the middle fragment says “from my sister” where the known input says
“for my sister”; the split input did not recover a complete book sentence.
This is one synthetic-speech journey and does not establish frequency or
human-microphone transcription quality.

At 01:49:47.769 the harness restored both media-device methods, disconnected
its observer, stopped sources and all stream tracks, closed its AudioContext
and deleted its page variable. CDP Network observation was disabled; reload
cleared page state. The owned fixture and tab were removed/closed after proof.
The disposable note remains for subsequent slices. Product source is unchanged.
