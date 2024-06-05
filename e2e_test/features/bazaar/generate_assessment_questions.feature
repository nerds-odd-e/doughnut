
Feature: Bazaar generate
  As a trainer, I want to generate to assessment questions in the Bazaar so that I can
  print the questions for notebook.

Background:

  Scenario: open pop up for log in if the user is not logged in and generate assessment
    Given I haven't login
    And there are some notes for existing user "another_old_learner"
      | topicConstructor | testingParent  | skipReview             |
      | LeSS in Action   |                | true|
      | team             | LeSS in Action |                     |
      | tech             | LeSS in Action |                     |
      | airgile          | LeSS in Action |                     |
      | scrum            | LeSS in Action |                     |
      | PO               | LeSS in Action |                     |
    And notebook "LeSS in Action" is shared to the Bazaar
    When I go to the bazaar
    And I generate assessment questions on notebook "LeSS in Action"
    Then I should see message that says "Please login first"

 Scenario Outline: display assessment questions from notebook
   Given I am logged in as an existing user
   And there are <notes count> notes under notebook "LeSS in Action" for the user
   And notebook "LeSS in Action" is shared to the Bazaar
   When I go to the bazaar
   And I generate assessment questions on notebook "LeSS in Action"
   Then I should see message that says "<message>"

   Examples:
   | notes count  | message                       |
   | 5            | Assessment For LeSS in Action |
   | 6            | Assessment For LeSS in Action |
    # | 4            | Insufficient notes            |

  @ignore
  Scenario: generate questions from notebook
    Given I am logged in as an existing user
    When I click on generate assessment questions button
    Then generate "5" questions

  @ignore
  Scenario: show error message if there are less than 5 notes in a notebook when generating questions
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent  | details             |
      | LeSS in Action   |                | An awesome training |
      | team             | LeSS in Action |                     |
      | tech             | LeSS in Action |                     |
    When I click on generate assessment questions button
    Then display error message that says ""

