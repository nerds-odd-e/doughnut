Feature: View Translate on Notes
    As a user I want to see translation of the Note

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
        | title            | testingParent | description |
        | English        |               | English Language     |
        And I click on the overview button of note "English"

    Scenario: Should show default title and translation button
        Then Note title will be shown "English" version
        And I should see button "ID"


        @ignore
    Scenario: Should translate note's content
        Given I am on Notes Detail
        When I click on ID/ENG button and ID/ENG translation is available
        Then Title and Description for Notes and its children will be translated to ID/ENG