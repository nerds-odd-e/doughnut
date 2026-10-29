# Independent source review

Launched `/root/oauth_source_review` on 2026-10-29 with `fork_turns: none`. The launch supplied only the selected story's Goal, Scope, key examples, evidence requirements, exclusions, provenance and explicit 2026-10-28/29 owner amendments. It instructed the reviewer to read no files, change nothing, derive individual commitments, and neither allocate slices nor accept proof. A follow-up requested splitting compound evidence fields and outcomes; it supplied no product, plan, implementation or proof. The returned commitments below retain that final numbered return. Source: [selected story](../../seeds/SEED-090-commissioned-learning-via-mcp.md#story-2). The former review predates the repair/release amendment and is superseded for acceptance by this fresh return.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Correct Donut’s declaration of recording scopes. | slice 6 | G4 |
| 2 | Correct the permission-upgrade response. | slice 6 | observed: AssistantLearningSessionTest.readOnlyAssistantIsChallengedForRecordingPermissionWithoutChangingLearningState asserts the exact structured insufficient_scope challenge |
| 3 | Prove the corrected contracts at the MCP/OAuth boundary. | slice 6 | observed: [Slice 6 accepted boundary proof](oauth-correction-evidence.md#slice-6-accepted-boundary-proof--2026-10-29); full backend suite passed, 2981 tests, zero failures/errors, two skipped |
| 4 | Release the tested correction through the application release workflow. | slice 7 | observed: oauth-correction-evidence.md forward release; v1.3.62, release job 123195649747 succeeded, exact build 8e90688 verified by health and artifact/metadata smoke |
| 5 | Continue the journey through the existing Donut plugin after release. | slice 1 | observed: fresh-chat-evidence.md native Donut Request and Report after v1.3.62 |
| 6 | Verify a real OAuth connection to an operator-authorized HTTPS deployment exposing Request and Report. | slice 1 | observed: fresh-chat-evidence.md authorized production origin/build, native matching Request and successful Report in the explicitly approved fresh conversations |
| 7 | Request both `notes:read` and `learning_session:record`. | slice 1 | observed: fresh-chat-evidence.md load-balancer actual scope string learning_session:record notes:read |
| 8 | Observe consent wording that describes recording. | slice 1 | observed: fresh-chat-evidence.md parent-inspected consent heading and recording explanation |
| 9 | Verify that the authorized connection can request sessions. | slice 1 | observed: fresh-chat-evidence.md matching notebook-15 Request with timezone Asia/Singapore |
| 10 | Verify that the authorized connection can record completed items. | slice 2 | observed: full-session-evidence.md native Report followed by one matching new saved recall log 139509 with HARD(2) and exact descriptive Feedback |
| 11 | Require fresh authorization for both scopes when upgrading an existing read-only connection. | slice 1 | observed: fresh-chat-evidence.md corrected existing registration then fresh dual-scope consent; Terry clicked Allow |
| 12 | Preserve read-only access until informed authorization grants both scopes. | slice 6 | observed: read-only challenge test retains zero logs and unchanged due date; upgrade test records Feedback only after fresh consent-issued token |
| 13 | Never widen an existing token. | slice 6 | observed: recordingChallengeLeadsToFreshConsentAndSavedFeedbackWithoutWideningTheOldToken reuses the old token and receives a challenge while history retains one Feedback |
| 14 | Never bypass consent. | slice 6 | observed: upgrade test consumes challenge scopes through approvedCode → begin → consent-context → Allow → exchange; AssistantConsentTest checks exact consent scopes and CSRF/denial proof ran |
| 15 | Have ChatGPT obtain the first available Request using the learner’s IANA timezone. | slice 2 | observed: full-session-evidence.md completed native Request with Asia/Singapore, sole なにしろ item reported in the chat |
| 16 | Follow the Request’s tutoring instructions. | slice 2 | observed: full-session-evidence.md Japanese voice tutoring from the reported item, learner-start wait, actual answers and matching Report; initial explanatory error was corrected and retained |
| 17 | Submit the existing Report markdown for completed items at normal finish. | slice 2 | observed: full-session-evidence.md exact existing Report markdown in native call after Terry asked to submit his result |
| 18 | Submit the existing Report markdown for completed items after early end. | slice 3 |  |
| 19 | Compare the returned recording result with actual Donut Feedback history. | slice 2 | G5 |
| 20 | Verify that incomplete items retain their prior Feedback. | slice 3 |  |
| 21 | Verify that incomplete items retain their prior schedule. | slice 3 |  |
| 22.1 | Retain each round’s date. | slice 5 |  |
| 22.2 | Retain each round’s origin. | slice 5 |  |
| 22.3 | Retain each round’s revision. | slice 5 |  |
| 22.4 | Retain each round’s account setup. | slice 5 |  |
| 22.5 | Retain each round’s data setup. | slice 5 |  |
| 22.6 | Retain each round’s timezone. | slice 5 |  |
| 22.7 | Retain each round’s actual scope string. | slice 5 |  |
| 22.8 | Retain each round’s consent wording. | slice 5 |  |
| 22.9 | Retain each round’s tool arguments. | slice 5 |  |
| 22.10 | Retain each round’s tool results. | slice 5 |  |
| 22.11 | Retain each round’s history comparisons. | slice 5 |  |
| 22.12 | Retain each round’s schedule comparisons. | slice 5 |  |
| 23 | Retain failures and unreached steps. | slice 5 |  |
| 24 | Update the existing guide using observed setup and session steps only. | slice 5 |  |
| 25 | Keep credentials out of retained evidence and the guide. | slice 5 |  |
| 26.1 | The first probe establishes the actual HTTPS origin. | slice 1 | observed: fresh-chat-evidence.md production origin https://doughnut.odd-e.com |
| 26.2 | The first probe establishes the actual revision. | slice 1 | observed: fresh-chat-evidence.md deployed v1.3.62 build 8e90688 confirmed again at the after-state reading |
| 26.3 | The first probe establishes the actual scopes. | slice 1 | observed: fresh-chat-evidence.md actual authorization scopes and corrected stored allowlist |
| 26.4 | The first probe establishes the actual client registration. | slice 1 | observed: fresh-chat-evidence.md Donut in ChatGPT existing registration fingerprint 97130bd51a6c, before/after read and one-client correction |
| 26.5 | The first probe establishes suitable data. | slice 1 | observed: fresh-chat-evidence.md notebook 15 sole item なにしろ, tracker 130239 and seven-entry baseline |
| 27 | Use ordinary supported operations on owner-approved data. | slice 5 |  |
| 28 | Credentials remain in the connection UI. | slice 5 |  |
| 29 | The connected Donut plugin counts as the ChatGPT surface; a separate ChatGPT application is unnecessary. | slice 1 | observed: fresh-chat-evidence.md connected Donut plugin used in Codex chats; no separate ChatGPT application |
| 30 | The assistant runs available plugin tools. | slice 1 | observed: fresh-chat-evidence.md native request_learning_session and record_learning_session markers and arguments |
| 31 | Terry supplies learning responses. | slice 2 | observed: full-session-evidence.md voice transcript contains Terry’s actual learning responses |
| 32.1 | Terry supplies connection UI observations unavailable through tools. | slice 1 | observed: fresh-chat-evidence.md Terry authorized new verification chats and reported Allow; parent-inspected setup observations are retained |
| 32.2 | Terry supplies consent observations unavailable through tools. | slice 1 | observed: fresh-chat-evidence.md consent screenshot plus Terry’s I clicked Allow observation |
| 33 | Automated MCP/OAuth/SDK evidence establishes only its own boundary, not the ChatGPT journey. | slice 5 |  |
| 34 | Deployment and automated proof do not establish ChatGPT success. | slice 7 | observed: oauth-correction-evidence.md separate release proof; fresh-chat-evidence.md separately observes native success after the retained invalid_scope failure |
| 35 | Failed or unavailable probes leave affected examples unverified and inform remaining interoperability refinement. | slice 5 |  |
| 36 | A sole Japanese notebook with an available session produces Japanese tutoring. | slice 2 | observed: full-session-evidence.md sole Japanese item and Japanese tutoring observed in the supplied chat |
| 37 | Tutoring waits for the learner to start. | slice 2 | observed: full-session-evidence.md preparation invites 始めよう, voice transcript starts じゃあ始めましょうか before tutoring |
| 38.1 | Completed items produce recorded Grades. | slice 2 | observed: full-session-evidence.md new recall log 139509 stores HARD, Grade.HARD(2), matching the submitted Grade 2 |
| 38.2 | Completed items produce recorded descriptive Feedback. | slice 2 | observed: full-session-evidence.md saved tutorFeedback exactly equals all submitted descriptive text |
| 38.3 | Recorded Grades are visible in history. | slice 2 | observed: full-session-evidence.md actual Donut tracker page visibly renders the new HARD entry; Grade.HARD(2) explains its label |
| 38.4 | Recorded descriptive Feedback is visible in history. | slice 2 | observed: full-session-evidence.md actual tracker page visibly renders the complete matching descriptive Feedback |
| 39 | For Request items A/B/C, completing A and then ending saves A-only Feedback. | slice 3 |  |
| 40.1 | After that early end, B’s history matches its baseline. | slice 3 |  |
| 40.2 | After that early end, B’s schedule matches its baseline. | slice 3 |  |
| 40.3 | After that early end, C’s history matches its baseline. | slice 3 |  |
| 40.4 | After that early end, C’s schedule matches its baseline. | slice 3 |  |
| 41 | Immediately ending an available Request submits an empty Report. | slice 4 | observed: fresh-chat-evidence.md user-selected immediate end, matching available Request and exact raw-delimiter native empty Report |
| 42 | The immediate-end result records zero items. | slice 4 | observed: fresh-chat-evidence.md clean native result recordedItems:[] and rejectedEntries:[] |
| 43.1 | Immediate end leaves all history unchanged. | slice 4 | observed: fresh-chat-evidence.md complete seven-entry history equals pre-call baseline |
| 43.2 | Immediate end leaves all schedules unchanged. | slice 4 | observed: fresh-chat-evidence.md complete sole-item tracker including schedule equals pre-call baseline |
| 44 | No due commissioned items produces an explicit no-session result. | slice 5 |  |
| 45 | ChatGPT explains that no-session result. | slice 5 |  |
| 46 | Notebook/program choice is deferred. | slice 5 |  |
| 47 | Automatic further sessions are deferred. | slice 5 |  |
| 48 | A replacement protocol is deferred. | slice 5 |  |
| 49 | General note editing is deferred. | slice 5 |  |
| 50 | Additional clients are deferred. | slice 5 |  |
| 51 | New real-client activation is deferred. | slice 5 |  |
| 52 | The journey moved from former implementation slice 4 by the owner’s 2026-10-28 direction. | slice 5 |  |
| 53 | The 2026-10-29 amendment adds the integration correction and application release. | slice 6 | observed: selected story Owner amendment — 2026-10-29 and the user’s explicit repair/release instruction |
| 54 | Earlier accepted MCP/OAuth evidence is reusable only within its original boundary. | slice 5 |  |
