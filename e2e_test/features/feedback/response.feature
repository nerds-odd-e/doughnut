Feature: Response to a message
  As a notebook owner or examinee, I want to respond to a message.

  Background:
    Given I am logged in as an existing user

  Scenario: Provide response as a Notebook Owner
    And there is a certified notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar
    And Pete has given the feedback "I don't understand this question" on a question on notebook "Just say 'Yes'"
    And I visit the feedback page
    When I open that conversation
    # Then I should be able to respond

  Scenario: No feedback
    Given I visit the feedback page
    Then I see the message "There is no feedback currently."

  @ignore
  Scenario Outline: Only the creator of a question or the person giving the feedback can see the conversation
    And there is a certified notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar
    When I begin the assessment from the "Just say 'Yes'" notebook in the bazaar
    And I answer the question wrongly
    And I submit feedback saying 'I believe the question is incorrect'

    Then "<User>" "<CanOrCannotSeeConversation>" see the conversation about question "Question 5"
    Examples:
      | User                | CanOrCannotSeeConversation |
      | old_learner         | can                        |
      | another_old_learner | cannot                     |
      | a_trainer           | can                        |

  @ignore
  Scenario Outline: I can see my conversation partners name
    And there is a certified notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar
    And Pete has given the feedback "I don't understand this question" on a question on notebook "Just say 'Yes'"
