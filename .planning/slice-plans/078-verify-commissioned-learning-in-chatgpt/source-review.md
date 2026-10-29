# Independent source review

Launched `/root/oauth_source_review` on 2026-10-29 with `fork_turns: none`. The launch supplied only the selected story's Goal, Scope, key examples, evidence requirements, exclusions, provenance and explicit 2026-10-28/29 owner amendments. It instructed the reviewer to read no files, change nothing, derive individual commitments, and neither allocate slices nor accept proof. A follow-up requested splitting compound evidence fields and outcomes; it supplied no product, plan, implementation or proof. The returned commitments below retain that final numbered return. Source: [selected story](../../seeds/SEED-090-commissioned-learning-via-mcp.md#story-2). The former review predates the repair/release amendment and is superseded for acceptance by this fresh return.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Correct Donut’s declaration of recording scopes. | slice 6 | G4 |
| 2 | Correct the permission-upgrade response. | slice 6 | observed: AssistantLearningSessionTest.readOnlyAssistantIsChallengedForRecordingPermissionWithoutChangingLearningState asserts the exact structured insufficient_scope challenge |
| 3 | Prove the corrected contracts at the MCP/OAuth boundary. | slice 6 | observed: [Slice 6 accepted boundary proof](oauth-correction-evidence.md#slice-6-accepted-boundary-proof--2026-10-29); full backend suite passed, 2981 tests, zero failures/errors, two skipped |
| 4 | Release the tested correction through the application release workflow. | slice 7 |  |
| 5 | Continue the journey through the existing Donut plugin after release. | slice 1 |  |
| 6 | Verify a real OAuth connection to an operator-authorized HTTPS deployment exposing Request and Report. | slice 1 |  |
| 7 | Request both `notes:read` and `learning_session:record`. | slice 1 |  |
| 8 | Observe consent wording that describes recording. | slice 1 |  |
| 9 | Verify that the authorized connection can request sessions. | slice 1 |  |
| 10 | Verify that the authorized connection can record completed items. | slice 2 |  |
| 11 | Require fresh authorization for both scopes when upgrading an existing read-only connection. | slice 1 |  |
| 12 | Preserve read-only access until informed authorization grants both scopes. | slice 6 | observed: read-only challenge test retains zero logs and unchanged due date; upgrade test records Feedback only after fresh consent-issued token |
| 13 | Never widen an existing token. | slice 6 | observed: recordingChallengeLeadsToFreshConsentAndSavedFeedbackWithoutWideningTheOldToken reuses the old token and receives a challenge while history retains one Feedback |
| 14 | Never bypass consent. | slice 6 | observed: upgrade test consumes challenge scopes through approvedCode → begin → consent-context → Allow → exchange; AssistantConsentTest checks exact consent scopes and CSRF/denial proof ran |
| 15 | Have ChatGPT obtain the first available Request using the learner’s IANA timezone. | slice 2 |  |
| 16 | Follow the Request’s tutoring instructions. | slice 2 |  |
| 17 | Submit the existing Report markdown for completed items at normal finish. | slice 2 |  |
| 18 | Submit the existing Report markdown for completed items after early end. | slice 3 |  |
| 19 | Compare the returned recording result with actual Donut Feedback history. | slice 2 |  |
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
| 26.1 | The first probe establishes the actual HTTPS origin. | slice 1 |  |
| 26.2 | The first probe establishes the actual revision. | slice 1 |  |
| 26.3 | The first probe establishes the actual scopes. | slice 1 |  |
| 26.4 | The first probe establishes the actual client registration. | slice 1 |  |
| 26.5 | The first probe establishes suitable data. | slice 1 |  |
| 27 | Use ordinary supported operations on owner-approved data. | slice 5 |  |
| 28 | Credentials remain in the connection UI. | slice 5 |  |
| 29 | The connected Donut plugin counts as the ChatGPT surface; a separate ChatGPT application is unnecessary. | slice 1 |  |
| 30 | The assistant runs available plugin tools. | slice 1 |  |
| 31 | Terry supplies learning responses. | slice 2 |  |
| 32.1 | Terry supplies connection UI observations unavailable through tools. | slice 1 |  |
| 32.2 | Terry supplies consent observations unavailable through tools. | slice 1 |  |
| 33 | Automated MCP/OAuth/SDK evidence establishes only its own boundary, not the ChatGPT journey. | slice 5 |  |
| 34 | Deployment and automated proof do not establish ChatGPT success. | slice 7 |  |
| 35 | Failed or unavailable probes leave affected examples unverified and inform remaining interoperability refinement. | slice 5 |  |
| 36 | A sole Japanese notebook with an available session produces Japanese tutoring. | slice 2 |  |
| 37 | Tutoring waits for the learner to start. | slice 2 |  |
| 38.1 | Completed items produce recorded Grades. | slice 2 |  |
| 38.2 | Completed items produce recorded descriptive Feedback. | slice 2 |  |
| 38.3 | Recorded Grades are visible in history. | slice 2 |  |
| 38.4 | Recorded descriptive Feedback is visible in history. | slice 2 |  |
| 39 | For Request items A/B/C, completing A and then ending saves A-only Feedback. | slice 3 |  |
| 40.1 | After that early end, B’s history matches its baseline. | slice 3 |  |
| 40.2 | After that early end, B’s schedule matches its baseline. | slice 3 |  |
| 40.3 | After that early end, C’s history matches its baseline. | slice 3 |  |
| 40.4 | After that early end, C’s schedule matches its baseline. | slice 3 |  |
| 41 | Immediately ending an available Request submits an empty Report. | slice 4 |  |
| 42 | The immediate-end result records zero items. | slice 4 |  |
| 43.1 | Immediate end leaves all history unchanged. | slice 4 |  |
| 43.2 | Immediate end leaves all schedules unchanged. | slice 4 |  |
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
