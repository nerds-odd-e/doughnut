Feature: Note display
  As a learner, I want to read my note or show my note
  to other people.

  Background:
    Given I am logged in as an existing user

  Scenario: Long details is abbreviated in card view
    Given I have a notebook with the head note "Potentially shippable product increment" and details "The output of every Sprint is called a Potentially Shippable Product Increment. The work of all the teams must be integrated before the end of every Sprint—the integration must be done during the Sprint."
    Then I should see these notes belonging to the user at the top level of all my notes
      | Title                                   | Details                                               |
      | Potentially shippable product increment | The output of every Sprint is called a Potentially... |
