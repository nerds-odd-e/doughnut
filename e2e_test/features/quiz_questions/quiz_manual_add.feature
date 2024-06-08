Feature: User Manually Add Questionsaaaa
  As a user, I want to add to be able to add new questions manually from the note page.

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "The cow joke"

  Scenario: Manually add a question to the note successfully
    When I add the following question for the note "The cow joke":
      | Question                             | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What do you call a cow with not leg? | Ground beef    | Cowboy             | Oxford             |
    Then I should see the question in the question list of the note "The cow joke":
      | Question                             | Correct Choice |
      | What do you call a cow with not leg? | Ground beef    |

  @ignore
  Scenario: Manually add questions to the note without answer should show error and not add question
    Given I add the following question for the note "team":
      | Question                                            | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the most common scuba diving certification? |                | Divemaster         | Open Water Diver   |
    Then I should see an error message with the text
      | errorMsg                                         |
      | Error: No correct answer given for the question. |
    And I should not be able to see the question in the question list of the note
      | note-topic | Question                                            |
      | team       | What is the most common scuba diving certification? |

  @ignore
  Scenario: Manually add questions to the note with less than 2 options should show error and not add question
    Given I add the following question for the note "team":
      | Question                                            | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the most common scuba diving certification? | Rescue Diver   |                    |                    |
    Then I should see an error message with the text
      | errorMsg                                                                         |
      | Error: Not enough options given for the question, should have at least 2 options |
    And I should not be able to see the question in the question list of the note
      | note-topic | Question                                            |
      | team       | What is the most common scuba diving certification? |

  @ignore
  Scenario: Manually add questions to the note that does not belong to user should show error and not add question
    Given I have note "NotMyNote" opened
    When I add the question with the following:
      | Question                                            | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |
    Then I should see an error message with the text
      | errorMsg                                                                 |
      | Error: This note does not belong to you. Please contact the note author. |
    And I should not be able to see the question in the question list of the note
      | note-topic | Question                                            |
      | team       | What is the most common scuba diving certification? |

  @ignore
  Scenario: When the user is logged out, when user manually add questions to the note should show error message and not add question
    Given I logged out
    And I have note "team" opened
    When I add the question with the following:
      | Question                                            | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |
    Then I should see an error message with the text
      | errorMsg                                                       |
      | Error: You are logged out, please log in to use this function. |
    And I should not be able to see the question in the question list of the note
      | note-topic | Question                                            |
      | team       | What is the most common scuba diving certification? |
