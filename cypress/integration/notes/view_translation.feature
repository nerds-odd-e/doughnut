Feature: View Translate on Notes
    As a user I want to see translation of the Note

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
        | title    |  titleIDN       | description          | descriptionIDN |
        | English  |  Indonesia      | English Language     | Bahasa Indonesia |
        And I click on the overview button of note "English"

    @featureToggle
    Scenario: Should show English as default language
        Then Note title will be shown "English"
        And Note description will be shown "English Language"
        And I should see translation button with language code "ID"


    @featureToggle
    Scenario: Switch to Indonesia
        When I switch language to "ID"
        Then Note title will be shown "Indonesia"
        And Note description will be shown "Bahasa Indonesia"
        And I should see translation button with language code "EN"

    @featureToggle
    Scenario: Switch Back to English
        Given I switch language to "ID"
        When I switch language to "EN"
        Then Note title will be shown "English"
        And Note description will be shown "English Language"
        And I should see translation button with language code "ID"