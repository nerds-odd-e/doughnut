@usingMockedOpenAiService
Feature: Recall stats pace comparison
  As a learner, I want to see whether today's recall pace is faster or
  slower than my usual pace for the same items, so I can gauge whether I'm
  off my game this morning.

  Background:
    Given I am logged in as an existing user
    And OpenAI evaluates the question as legitimate
    And I have a notebook "English practice" with notes:
      | Title    | Content                        |
      | sedition | Sedition means incite violence |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   | to stay silent     |
    And the note "sedition" was assimilated on day 1

  # This is new E2E ground (no prior /settings/recall-stats coverage to
  # extend). Root cause found: `Answer.createdAt` is set from
  # `System.currentTimeMillis()` (real wall clock), but
  # `RecallStatsService.compute`'s query window bounds derive from
  # `testabilitySettings.getCurrentUTCTimestamp()` (the simulated
  # backend-time-travel clock this scenario uses to fabricate "day 2..4").
  # Those are two different clocks in this environment: every answer this
  # scenario records is real-wall-clock-stamped, while the stats query's
  # [startTime, endTime) window is computed relative to the simulated day,
  # so the answers fall outside it and totalReviewsAllTime comes back 0 -
  # same category of clock mismatch already documented on the detour
  # scenario in recall_timing.feature. Fixing this would mean either making
  # Answer.createdAt testability-clock-aware (a behavior change well beyond
  # this slice) or adding a dedicated backdating testability endpoint - both
  # out of scope here. Left @wip (CI skips it); backend and frontend unit
  # tests (RecallStatsServiceTest.Pace, PaceTile.spec.ts,
  # RecallStatsSettingsTab.spec.ts) are the primary verification for this
  # slice.
  @wip
  Scenario: Today's much-slower answer shows a slower-than-usual pace
    When I answer "sedition" slowly with thinking time 5000 ms over 2 days since day 2
    And on day 4 I answer "sedition" slowly with thinking time 30000 ms
    And I visit my recall stats
    Then I should see today's pace is slower than usual
