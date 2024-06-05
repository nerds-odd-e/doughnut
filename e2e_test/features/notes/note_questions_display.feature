@ignore
Feature: Questions display
  As a notebook owner, I want to see a list of questions for each note and their approval status.

  Background:
    Given I am logged in as an existing user

  Scenario: new page with a the title of the note as title and a list of questions.
    Given a notebook with notes
      | topicConstructor                        | details                                                                                                                                                                                                     |
      | Potentially shippable product increment | The output of every Sprint is called a Potentially Shippable Product Increment. The work of all the teams must be integrated before the end of every Sprint—the integration must be done during the Sprint. |
    Then I should see a button that shows a new page to show all the questions attached to this note
