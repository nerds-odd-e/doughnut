Feature: View Translate on Notes
    As a user I want to see translation of the Note

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
        | title   |  titleIDN       | testingParent | description |
        | English  |  Indonesia    |               | English Language     |
        And I click on the overview button of note "English"

    @featureToggle
    Scenario: Should show default title and translation button
        Then Note title will be shown "English" version
        And I should see button "ID"


    @featureToggle
    Scenario: Should show translation title and translation button changes
        When I click on the translation button "ID"
        Then Note title will be shown "Indonesia" version
        And I should see button "EN"
        When I click on the translation button "EN"
        Then Note title will be shown "English" version
        And I should see button "ID"