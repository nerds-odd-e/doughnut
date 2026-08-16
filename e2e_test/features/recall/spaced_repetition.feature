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
    And On day 3 I recall "Note 2, Note 1, end " and assimilate new "Note 3, end "
    And On day 4 I recall "Note 3, Note 2, end " and assimilate new "end         "
    And On day 5 I recall "Note 3, end         " and assimilate new "end         "
    And On day 6 I recall "end                 " and assimilate new "end         "
    And On day 7 I recall "end                 " and assimilate new "end         "
    And On day 8 I recall "Note 1, end         " and assimilate new "end         "
    And On day 9 I recall "Note 2, end         " and assimilate new "end         "

  @mockBrowserTime
  Scenario: Memory Tracker shows Stability and Again Difficulty after incorrect just-review
    Given the browser and backend are on day 1
    When I assimilate the note "Note 1"
    And I am recalling my note on day 1
    And I choose yes I remember
    And I am recalling my note on day 2
    And I choose no I need more recall
    And I visit the understanding memory tracker for "Note 1"
    Then I should see Stability 8
    And I should see 12 hours between last and next recall
    And I should see Difficulty 10

  @mockBrowserTime
  Scenario: Strictly follow the schedule but want to recall more
    When On day 1 I recall "                    " and assimilate new "Note 1, end "
    And On day 2 I recall "Note 1, end         " and assimilate new "Note 2, end "
    And On day 3 I recall "Note 2, Note 1, end " and assimilate new "Note 3, end "
    And On day 4 I recall "Note 3, Note 2, end " and assimilate new "end         "
    And I ask to do more recall
    And I repeat more old "Note 3         "
    Then I should have "0/0/3" note for assimilation and "6/6/3" for recall
