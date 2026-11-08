@disableOpenAiService
Feature: Spreading out the recall backlog
  As a learner returning to a large recall backlog, I want to spread it over
  the coming days so that today's workload is manageable again.

  @mockBrowserTime
  Scenario: Spread out the recall backlog from settings
    Given I am logged in as an existing user
    And my daily new notes to assimilate is set to 3
    And there are notes from Note 1 to Note 3
    And On day 1 I self-review "end" and assimilate new "Note 1, Note 2, Note 3, end"
    And It's day 10
    When I return and spread out my recall backlog from my recall stats
    Then the recall forecast should show 1 today and 1 on each of the next 2 days
    And the menu should show 1 note to recall
