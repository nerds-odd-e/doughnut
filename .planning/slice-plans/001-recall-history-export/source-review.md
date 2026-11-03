# Source review: SEED-100#story-1

Launch: fresh general-purpose agent, no conversation inheritance, 2026-11-03, during slice 1 delegation.
Input: the story-1 section of `.planning/seeds/SEED-100-recall-history-export.md` from **Goal:** through **UI:** (Goal, Scope, Deferred promises, Out of scope, Key examples, UI), verbatim; told to read no other files, change nothing, and return numbered commitments with compound items split. No plan, return, or product was supplied.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Goal: A signed-in learner can get one JSON file holding every recall event they have and every daily probe they took. | slice 3 | |
| 1a | Goal: The file is usable by the learner, or an agent working for them, to study recall-session efficiency, repetition efficiency, and how the morning probe relates to that day's recall. | slice 3 | |
| 1b | Goal: The export is the data source for story 2. | slice 3 | |
| X1 | Goal (exclusion): The export itself makes no analysis. | slice 1 | observed: RecallHistoryExportService.export maps rows to fields only; no derived statistics |
| 2 | Who and how: Only the current user's own data is exported. | slice 1 | observed: RecallLogRepository.findRecallEventRows WHERE mt.user.id = :userId; controller passes current user |
| 2a | Who and how: Two ways in give one (the same) response. | slice 3 | |
| 2a.1 | Who and how: Way in 1: a download control on the recall stats settings tab. | slice 3 | |
| 2a.2 | Who and how: Way in 2: an authenticated `GET` under `/api`. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAgentWithTheLearnersTokenGetsTheJson: GET /api/user/recall-history-export → 200 |
| 2a.3 | Who and how: The `GET` is described in OpenAPI. | slice 2 | G1 |
| 2a.4 | Who and how: An agent calls the `GET` with the user's existing API token (`Authorization: Bearer`, as generated on the settings page). | slice 1 | observed: UserRecallHistoryExportControllerTest.anAgentWithTheLearnersTokenGetsTheJson: Bearer user token → 200 JSON |
| X2 | Who and how (exclusion): No new authentication. | slice 1 | observed: diff adds no auth code; endpoint uses existing assertLoggedIn |
| X3 | Who and how (exclusion): No MCP tool. | slice 1 | observed: diff adds no MCP tool |
| X4 | Who and how (exclusion): No CLI command. | slice 1 | observed: diff adds no CLI command |
| 3 | Timezone: The request names an IANA timezone, as the recall stats request already does. | slice 1 | observed: UserController.exportRecallHistory required timezone param via TimezoneUtils.parseTimezone |
| 3a | Timezone: The page sends the browser's timezone. | slice 3 | |
| 3b.1 | Timezone: The file records that timezone. | slice 1 | observed: UserRecallHistoryExportControllerTest.namesTheTimezoneAndTheExportTimeInThatZone: timezone Asia/Bangkok |
| 3b.2 | Timezone: The file records the export time. | slice 1 | observed: UserRecallHistoryExportControllerTest.namesTheTimezoneAndTheExportTimeInThatZone: exportedAt 1989-01-02T15:00:00+07:00 |
| 3c | Timezone: Every timestamp in the file is ISO-8601 with that zone's offset, so "local day" and "morning" can be read directly. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials completedAt +07:00; slice 1 event timestamps +07:00 |
| 4 | Recall events: One flat list of recall events. | slice 1 | observed: UserRecallHistoryExportControllerTest.listsEveryKindOfRecallEventOldestFirstInTheRequestedZone: recallEvents flat list |
| 4a | Recall events: One entry per recall log. | slice 1 | observed: UserRecallHistoryExportControllerTest.listsEveryKindOfRecallEventOldestFirstInTheRequestedZone: one event per log (3 logs → 3 events) |
| 4b | Recall events: Oldest first. | slice 1 | observed: UserRecallHistoryExportControllerTest.listsEveryKindOfRecallEventOldestFirstInTheRequestedZone: recordedAt ascending |
| 4c | Recall events: Covers the whole history (no time window). | slice 1 | observed: RecallLogRepository.findRecallEventRows has no time filter |
| 4d.1 | Recall events (each entry): Recorded time. | slice 1 | observed: UserRecallHistoryExportControllerTest.listsEveryKindOfRecallEventOldestFirstInTheRequestedZone: recordedAt |
| 4d.2 | Recall events (each entry): Grade (`AGAIN`/`HARD`/`GOOD`/`EASY`) or `CONFUSION`. | slice 1 | observed: UserRecallHistoryExportControllerTest.listsEveryKindOfRecallEventOldestFirstInTheRequestedZone grades; anAccidentalMatch... CONFUSION |
| 4d.3 | Recall events (each entry): Kind of event: answered recall prompt, just review, or tutor feedback. | slice 1 | observed: UserRecallHistoryExportControllerTest.listsEveryKindOfRecallEventOldestFirstInTheRequestedZone: kinds answered, just review, tutor feedback |
| 4d.4 | Recall events (each entry): For tutor feedback, the tutor feedback text. | slice 1 | observed: UserRecallHistoryExportControllerTest.listsEveryKindOfRecallEventOldestFirstInTheRequestedZone: tutorFeedback text |
| 4d.5 | Recall events (each entry): Elapsed hours before the event. | slice 1 | observed: UserRecallHistoryExportControllerTest.twoAnswersOnOneTracker...: elapsedHours 48 |
| 4d.6 | Recall events (each entry): Stability before the event. | slice 1 | observed: UserRecallHistoryExportControllerTest.twoAnswersOnOneTracker...: stabilityBefore equals state left |
| 4d.7 | Recall events (each entry): Difficulty before the event. | slice 1 | observed: UserRecallHistoryExportControllerTest.twoAnswersOnOneTracker...: difficultyBefore equals state left |
| 4d.8 | Recall events (each entry): Retrievability before the event. | slice 1 | observed: UserRecallHistoryExportControllerTest.twoAnswersOnOneTracker...: retrievability present |
| 4d.9 | Recall events (each entry): Memory tracker id. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPromptCarriesItsTrackerPromptAndAnswerDetails: memoryTrackerId |
| 4d.10 | Recall events (each entry): Memory tracker type. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPromptCarriesItsTrackerPromptAndAnswerDetails: memoryTrackerType |
| 4d.11 | Recall events (each entry): Note id. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPromptCarriesItsTrackerPromptAndAnswerDetails: noteId |
| 4d.12 | Recall events (each entry): (Purpose of 4d.9–4d.11) repetitions of one item can be grouped. | slice 1 | observed: UserRecallHistoryExportControllerTest.twoAnswersOnOneTracker...: shared tracker and note ids |
| 4e.1 | Recall events (answered-prompt entry): Question type (MCQ or spelling). | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPrompt... MCQ; anAccidentalMatch... SPELLING |
| 4e.2 | Recall events (answered-prompt entry): The prompt's created time. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPrompt...: promptCreatedAt present |
| 4e.3 | Recall events (answered-prompt entry): Answer id. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPrompt...: answer id |
| 4e.4 | Recall events (answered-prompt entry): Answer created time. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPrompt...: createdAt with +07:00 |
| 4e.5 | Recall events (answered-prompt entry): Answer thinking time. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPromptCarriesItsTrackerPromptAndAnswerDetails: thinking/away/detour/idle times and counts |
| 4e.6 | Recall events (answered-prompt entry): Answer away time. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPromptCarriesItsTrackerPromptAndAnswerDetails: thinking/away/detour/idle times and counts |
| 4e.7 | Recall events (answered-prompt entry): Answer detour time. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPromptCarriesItsTrackerPromptAndAnswerDetails: thinking/away/detour/idle times and counts |
| 4e.8 | Recall events (answered-prompt entry): Answer idle time. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPromptCarriesItsTrackerPromptAndAnswerDetails: thinking/away/detour/idle times and counts |
| 4e.9 | Recall events (answered-prompt entry): Answer away count. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPromptCarriesItsTrackerPromptAndAnswerDetails: thinking/away/detour/idle times and counts |
| 4e.10 | Recall events (answered-prompt entry): Answer detour count. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnsweredRecallPromptCarriesItsTrackerPromptAndAnswerDetails: thinking/away/detour/idle times and counts |
| 4e.11 | Recall events (answered-prompt entry): Answer outcome. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAccidentalMatchGivesAGradeAndAConfusionEventSharingTheAnswer: outcome ACCIDENTAL_MATCH |
| 4e.12 | Recall events (answered-prompt entry): Answer exclusion time. | slice 1 | observed: UserRecallHistoryExportControllerTest.anExcludedAnswerIsPresentWithItsExclusionTimeAndReason: excludedAt |
| 4e.13 | Recall events (answered-prompt entry): Answer exclusion reason. | slice 1 | observed: UserRecallHistoryExportControllerTest.anExcludedAnswerIsPresentWithItsExclusionTimeAndReason: exclusionReason |
| 4f | Recall events: One spelling answer can produce two events (a grade on the tracker under recall and a confusion on the note it accidentally named). | slice 1 | observed: UserRecallHistoryExportControllerTest.anAccidentalMatchGivesAGradeAndAConfusionEventSharingTheAnswer: AGAIN + CONFUSION events |
| 4f.1 | Recall events: Both such events carry the same answer id. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAccidentalMatchGivesAGradeAndAConfusionEventSharingTheAnswer: same answer id |
| 4g.1 | Recall events (inclusion): Excluded answers are included. | slice 1 | observed: UserRecallHistoryExportControllerTest.anExcludedAnswerIsPresentWithItsExclusionTimeAndReason |
| 4g.2 | Recall events (inclusion): Trackers removed from recall are included. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnswerOnATrackerRemovedFromRecallIsStillExported |
| 4g.3 | Recall events (inclusion): Notes in trash are included. | slice 1 | observed: UserRecallHistoryExportControllerTest.anAnswerOnANoteInTrashIsStillExportedWithThatNoteId |
| 4g.4 | Recall events: Analysis (not the export) decides what to leave out. | slice 1 | observed: RecallLogRepository.findRecallEventRows applies no exclusion filters |
| 5 | Daily probes: Every stored probe is exported. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: exactly the learner's 2 probes, other user's excluded |
| 5a | Daily probes: Oldest first. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: day 1 before day 3 though created in reverse order |
| 5b.1 | Daily probes (each probe): Completed time. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: completedAt, speed/accuracy/lapses/variability 3.5/85/2/0.4 |
| 5b.2 | Daily probes (each probe): Speed. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: completedAt, speed/accuracy/lapses/variability 3.5/85/2/0.4 |
| 5b.3 | Daily probes (each probe): Accuracy. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: completedAt, speed/accuracy/lapses/variability 3.5/85/2/0.4 |
| 5b.4 | Daily probes (each probe): Lapse count. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: completedAt, speed/accuracy/lapses/variability 3.5/85/2/0.4 |
| 5b.5 | Daily probes (each probe): Variability. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: completedAt, speed/accuracy/lapses/variability 3.5/85/2/0.4 |
| 5b.6 | Daily probes (each probe): Its trials. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: 20 trials |
| 5c.1 | Daily probes (each trial): Stimulus. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: first trial left/right/612/false |
| 5c.2 | Daily probes (each trial): Response. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: first trial left/right/612/false |
| 5c.3 | Daily probes (each trial): Response time. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: first trial left/right/612/false |
| 5c.4 | Daily probes (each trial): Correct. | slice 2 | observed: UserRecallHistoryExportControllerTest.listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials: first trial left/right/612/false |
| 5d | Daily probes: Probes are exported whether or not the daily probe is currently enabled. | slice 2 | observed: UserRecallHistoryExportControllerTest.probesTakenBeforeTheProbeWasTurnedOffAreStillExported |
| 6a | Empty parts: A user with no probes gets the same file with the probe list empty. | slice 2 | observed: UserRecallHistoryExportControllerTest.aLearnerWithoutProbesGetsRecallEventsAndAnEmptyProbeList |
| 6b | Empty parts: A user with no recall events gets the same file with the recall event list empty. | slice 1 | observed: UserRecallHistoryExportControllerTest.namesTheTimezoneAndTheExportTimeInThatZone: recallEvents empty |
| 7 | Required restriction: The request is refused for a caller who is not signed in or has no valid token (recall history is private learning data, ADR 0001). | slice 1 | observed: UserRecallHistoryExportControllerTest.requiresLogin and aRequestWithoutCredentialIsRefused: 401 |
| X5 | Deferred (not rejected): Note titles or other note content. | slice 1 | observed: RecallHistoryExport DTO carries no such field; query reads only recall logs of the current user |
| X6 | Deferred (not rejected): Recall prompts that were shown but never answered. | slice 1 | observed: RecallHistoryExport DTO carries no such field; query reads only recall logs of the current user |
| X7 | Deferred (not rejected): A session id or any other derived value. | slice 1 | observed: RecallHistoryExport DTO carries no such field; query reads only recall logs of the current user |
| X8 | Deferred (not rejected): CSV or other formats. | slice 3 | |
| X9 | Deferred (not rejected): A format-version field (one consumer, story 2, reads it once). | slice 1 | observed: RecallHistoryExport DTO carries no such field; query reads only recall logs of the current user |
| X10 | Out of scope: Any analysis, charts, or derived statistics. | slice 1 | observed: RecallHistoryExport DTO carries no such field; query reads only recall logs of the current user |
| X11 | Out of scope: Importing data. | slice 3 | |
| X12 | Out of scope: Exporting other users' data. | slice 1 | observed: RecallHistoryExport DTO carries no such field; query reads only recall logs of the current user |
| 8 | UI: On the recall stats tab, one control labeled as downloading the recall history. | slice 3 | |
| 8a | UI: The control is available whenever the tab is shown, including its empty state. | slice 3 | |
| 8b | UI: It saves a `.json` file. | slice 3 | |
| 8c | UI: The file name includes the export date. | slice 3 | |
| X13 | UI (exclusion): No preview. | slice 3 | |
| X14 | UI (exclusion): No options. | slice 3 | |
| E1 | Key examples: Learner with 3 answered recall prompts, 1 just review, 1 tutor feedback, 2 daily probes, in `Asia/Bangkok`, downloads from the recall stats tab → file holds 5 recall events and 2 probes in time order; timestamps end in `+07:00`; file names `Asia/Bangkok` and the export time. | slice 3 | |
| E2 | Key examples: Two answers on the same note's memory tracker, exported → both events carry the same memory tracker id and note id; the second has the stability and difficulty the first left. | slice 1 | observed: UserRecallHistoryExportControllerTest.twoAnswersOnOneTrackerShareItsIdsAndTheSecondStartsFromTheStateTheFirstLeft |
| E3 | Key examples: Learner who never took a daily probe exports → recall events present; probe list empty. | slice 2 | observed: UserRecallHistoryExportControllerTest.aLearnerWithoutProbesGetsRecallEventsAndAnEmptyProbeList |
| E4 | Key examples: Learner who took probes and later turned the daily probe off exports → the earlier probes are in the file. | slice 2 | observed: UserRecallHistoryExportControllerTest.probesTakenBeforeTheProbeWasTurnedOffAreStillExported |
| E5 | Key examples: Answer excluded as ambiguous, exported → its event is present with its exclusion time and reason. | slice 1 | observed: UserRecallHistoryExportControllerTest.anExcludedAnswerIsPresentWithItsExclusionTimeAndReason |
| E6a | Key examples: Agent with the learner's API token calls the endpoint with `timezone=Asia/Bangkok` → gets the same JSON the download gives. | slice 3 | |
| E6b | Key examples: Call without a token → refused; no data returned. | slice 1 | observed: UserRecallHistoryExportControllerTest.aRequestWithoutCredentialIsRefused: 401 |
