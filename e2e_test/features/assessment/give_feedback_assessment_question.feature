@ignore
Feature: Learner gives feedback on an assessment question
    As a learner, I want to give feedback on an assessment question

    Background:
        Given I am logged in as an existing user
        And I have a notebook with the head note "The cow joke"
        And there are questions in the notebook "The cow joke" for the note:
            | Note Topic   | Question             | Answer | One Wrong Choice |
            | The cow joke | What does a cow say? | moo    | woo              |

    Scenario: Starts an assessment and answers wrongly, then gives feedback on the question
        When I start an assessment on the note "The cow joke"
        And I answer the question "What does a cow say?" wrongly
        Then I see an option to give feedback on the question

    Scenario: I have the option to give feedback
        Given I answered the question "What does a cow say?" wrongly
        When I submit my feedback
        Then my feedback is saved and I see a confirmation message
