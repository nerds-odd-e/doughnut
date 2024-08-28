Feature: Response to a message
  As a notebook owner or examinee, I want to respond to a message.

  Background:
    Given I am logged in as an existing user

  Scenario: Provide response as a Notebook Owner
    Given I visit the feedback page
    # When I have received feedback on a question
    # And I open that conversation
    # Then I should be able to respond

  Scenario: No feedback
    Given I visit the feedback page
    # When I have no feedback
    Then I see the message "There is no feedback currently."
