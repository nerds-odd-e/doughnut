@disableOpenAiService
Feature: Spaced-repetition
  As a learner, I want to recall my notes in the most optimized way

  Background:
    Given I am logged in as an existing user
    And my daily new notes to assimilate is set to 1
    And there are notes from Note 1 to Note 3

  @mockBrowserTime
  Scenario: The assimilation and recall page
    Given On day 1 I should have "0/1/3" note for assimilation and "0/0/0" for recall
    And the browser and backend are on day 1
    When I assimilate the note "Note 1"
    Then On day 1 I should have "1/1/3" note for assimilation and "0/1/1" for recall
    And On day 2 I should have "1/2/3" note for assimilation and "0/1/1" for recall

  @mockBrowserTime
  Scenario: Strictly follow the schedule
    When On day 1 I recall "                    " and assimilate new "Note 1, end "
    And On day 2 I recall "Note 1, end         " and assimilate new "Note 2, end "
    And On day 3 I recall "Note 2, end         " and assimilate new "Note 3, end "
    And On day 4 I recall "Note 3, end         " and assimilate new "end         "
    And On day 5 I recall "Note 1, end         " and assimilate new "end         "
    And On day 6 I recall "Note 2, end         " and assimilate new "end         "
    And On day 7 I recall "Note 3, end         " and assimilate new "end         "
    And On day 8 I recall "end                 " and assimilate new "end         "
    And On day 9 I recall "end                 " and assimilate new "end         "

  @mockBrowserTime
  Scenario: Memory Tracker after assimilate has no last recall
    Given the browser and backend are on day 1
    When I assimilate the note "Note 1"
    And I visit the understanding memory tracker for "Note 1"
    Then I should see Last Recall Time "N/A"
    And I should see Next Recall Time equal to Assimilated Time
    And I should see Stability 0
    And I should see Difficulty "N/A"

  @mockBrowserTime
  Scenario: Remove from recall does not change Last Recall Time
    Given the browser and backend are on day 1
    When I assimilate the note "Note 1"
    And I am recalling my note on day 1
    And I choose yes I remember
    And I visit the understanding memory tracker for "Note 1"
    Then I record the current memory tracker schedule
    When I remove the memory tracker from recall
    Then the memory tracker should be skipped
    And I should see the same Last Recall Time
    When I revive the memory tracker on this page
    Then the memory tracker should be available for recall again
    And I should see the same Last Recall Time

  @mockBrowserTime
  Scenario: Memory Tracker shows a GOOD RecallLog after just-review Yes
    Given the browser and backend are on day 1
    When I assimilate the note "Note 1"
    And I am recalling my note on day 1
    And I choose yes I remember
    And I visit the understanding memory tracker for "Note 1"
    Then I should see a GOOD RecallLog with elapsed hours and no answer id

  @mockBrowserTime
  Scenario: Memory Tracker shows first Again after just-review No on New
    Given the browser and backend are on day 1
    When I assimilate the note "Note 1"
    And I am recalling my note on day 1
    And I choose no I need more recall
    And I visit the understanding memory tracker for "Note 1"
    Then I should see Stability 5
    And I should see Difficulty 6.4133
    And I should see 5 hours between last and next recall

  @mockBrowserTime
  Scenario: On-time Good after first Again uses short-term Stability 6
    Given the browser and backend are on day 1
    When I assimilate the note "Note 1"
    And I am recalling my note on day 1
    And I choose no I need more recall
    And It's day 1, 13 hour
    And I visit recall
    And I choose yes I remember
    And I visit the understanding memory tracker for "Note 1"
    Then I should see Stability 6
    And I should see 6 hours between last and next recall

  @mockBrowserTime
  Scenario: Memory Tracker shows Stability and Again Difficulty after incorrect just-review
    Given the browser and backend are on day 1
    When I assimilate the note "Note 1"
    And I am recalling my note on day 1
    And I choose yes I remember
    And It's day 3, 15 hour
    And I visit recall
    And I choose no I need more recall
    And I visit the understanding memory tracker for "Note 1"
    Then I should see Stability 15
    And I should see 15 hours between last and next recall
    And I should see Difficulty 7.3945
    And I should see an AGAIN RecallLog

  @mockBrowserTime
  Scenario: Same-hour Good after first Good stays Stability 55
    Given the browser and backend are on day 1
    When I assimilate the note "Note 1"
    And I am recalling my note on day 1
    And I choose yes I remember
    And I ask to do more recall
    And I choose yes I remember
    And I visit the understanding memory tracker for "Note 1"
    Then I should see Stability 55
    And I should see 55 hours between last and next recall

  @mockBrowserTime
  Scenario: Same-hour Again after first Good uses short-term Stability 18
    Given the browser and backend are on day 1
    When I assimilate the note "Note 1"
    And I am recalling my note on day 1
    And I choose yes I remember
    And I ask to do more recall
    And I choose no I need more recall
    And I visit the understanding memory tracker for "Note 1"
    Then I should see Stability 18
    And I should see 18 hours between last and next recall
    And I should see Difficulty 7.3945
    And I should see an AGAIN RecallLog

  @mockBrowserTime
  Scenario: Strictly follow the schedule but want to recall more
    When On day 1 I recall "                    " and assimilate new "Note 1, end "
    And On day 2 I recall "Note 1, end         " and assimilate new "Note 2, end "
    And On day 3 I recall "Note 2, end         " and assimilate new "Note 3, end "
    And On day 4 I recall "Note 3, end         " and assimilate new "end         "
    And I ask to do more recall
    And I repeat more old "Note 1, Note 2, Note 3"
    Then I should have "0/0/3" note for assimilation and "6/6/3" for recall
