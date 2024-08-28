Feature: Response to a message
  As a notebook owner or examinee, I want to respond to a message.

  Background:
    Given I am logged in as an existing user

  Scenario: Provide response as a Notebook Owner
    Given I visit the feedback page
    # When I have received feedback on a question
    # And I open that conversation
    # Then I should be able to respond

  @ignore
  Scenario: No feedback
    Given I visit the feedback page
    # When I have no feedback
    Then I see the message "There is no feedback currently."

  Scenario: Receive feedback on a question that I've created:
    Given I visit the feedback page
    And Pete has given the feedback I don't understand this question on "Question 5"
    # And I'm the creator of question 5
    When I open feedback on "Question 5"
    # Then I should see the feedback message "I don't understand this question"

  @ignore
  Scenario: Don't see feedback on a question that I haven't created:

    Given Pete has given the feedback "I don't understand this question" on question 5
    And I'm not the creator of question 5
    Then I don't see feedback from Pete
