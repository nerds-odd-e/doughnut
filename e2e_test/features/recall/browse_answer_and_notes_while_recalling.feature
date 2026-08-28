@mockBrowserTime
Feature: Browse answers and notes while recalling
  As a learner, I want to browse answers and notes while recalling
  so that I can pause recalling to review my answers and notes
  and go back to recalling when I am ready.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content                        |
      | English  |                                |
      | sedition | Sedition means incite violence |
      | sedation | Put to sleep is sedation       |
      | medical  |                                |
    And the notes "English" are skipped from the assimilation sequence
    And It's day 1

  @disableOpenAiService
  Scenario: View last answered question when the quiz answer was correct
    Given the note "sedition" was assimilated as spelling on day 1
    When I visit recall for a due recall prompt on day 2
    And I type my answer "sedition"
    Then I should see that my last spelling answer was correct with recall count 1

  @usingMockedOpenAiService
  Scenario: Viewing a previous answer does not count toward the current question's thinking time
    Given OpenAI evaluates the question as legitimate
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   | to stay silent     |
    And the note "sedition" was assimilated as spelling on day 1
    And the note "sedition" was assimilated on day 1
    When I visit recall waiting for 2 due recall prompts on day 2
    And I type my answer "sedition"
    Then I should be asked "What is the meaning of sedition?"
    When I view the last answered question for 5 seconds
    And I resume recalling
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to incite violence"
    And I visit the understanding memory tracker for "sedition"
    Then the recall history should show a thinking time under 2 seconds for that answer

  @disableOpenAiService
  @skipOptimizationDueToKnownNecessarySlowness
  Scenario: Browse notes while recalling and come back
    Given the note "sedition" was assimilated on day 1
    And I assimilate the note "sedition" with the option of remembering spelling
    When I visit recall for a due recall prompt on day 2
    And I type my answer "riot"
    Then I should see that my spelling answer "riot" is incorrect
    When I visit note "medical"
    Then I should be able to resume recalling
    When I resume recalling
    Then I should be back to the current question

  @usingMockedOpenAiService
  Scenario: Returning from a note in the same half-day keeps the unanswered prompt
    Given OpenAI evaluates the question as legitimate
    And OpenAI will return these questions in order:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   | to stay silent     |
      | What is the meaning of sedation? | to put to sleep    | to riot            | Open Water Diver   | to stay silent     |
    And the notes "sedition, sedation" are assimilated on day 1
    When I visit recall waiting for 2 due recall prompts on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I visit note "medical"
    And I return to recalling
    Then I should be asked "What is the meaning of sedition?"

  @disableOpenAiService
  Scenario: I can remove a note from further recalls
    Given the note "sedition" was assimilated on day 1
    And the note "sedition" was assimilated as spelling on day 1
    When I visit recall for a due recall prompt on day 2
    And I type my answer "sedition"
    And I choose to remove the last memory tracker from recalls
    Then On day 100 I should have "0/2/2" note for assimilation

  @disableOpenAiService
  Scenario: I can revive a memory tracker removed from recalls
    Given the note "sedition" was assimilated as spelling on day 1
    When I visit recall for a due recall prompt on day 2
    And I type my answer "sedition"
    And I choose to remove the last memory tracker from recalls
    And I revive the memory tracker on this page
    Then the memory tracker should be available for recall again
