Feature: User Manually Add Questionsaaaa
  As a user, I want to add to be able to add new questions manually from the note page.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent  | details             |
      | LeSS in Action   |                | An awesome training |
      | team             | LeSS in Action |                     |
      | tech             | LeSS in Action |                     |
  #And there are some notes that are not created for the current user:
  # | topicConstructor | testingParent  | details  |
  #| NotMyNote        | LeSS in Action | FunTimes |

  Scenario: Manually add questions to the note with valid question and answer should return success and add question
    Given I access the add question page for the note "team"
    When I add the question with the following:
      | Question                                            | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |
  #    Then I should be able to see a sucess message
  #    And I should be able to see the question in the qusestion list of the note
  #      | note-topic | Question                                      |
  #      | team       | What is the most common scuba diving certification? |

  @ignore
  Scenario: Manually add questions to the note without answer should show error and not add question
    Given I have note "team" opened
    When I add the question with the following:
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
    Given I have note "team" opened
    When I add the question with the following:
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

  @ignore
  Scenario: When user has added a question successfully to the note
    When I add the question with the following:
      | Question Stem                                       | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |
    Then I should see the question in the question list of the note
      | note-topic | question stem                                       |
      | team       | What is the most common scuba diving certification? |
