Feature: View Translate on Notes
    As a user I want to see translation of the Note

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
        | title    |  titleIDN       | description          | descriptionIDN |
        | English  |  Indonesia      | English Language     | Bahasa Indonesia |
        And I click on the overview button of note "English"

    @featureToggle
    Scenario: Note detail view show English as default language
        Then Note title will be shown "English"
        And Note description will be shown "English Language"
        And I should see translation button with language code "ID"


    @featureToggle
    Scenario: Note detail view switch to Indonesia
        When I switch language to "ID"
        Then Note title will be shown "Indonesia"
        And Note description will be shown "Bahasa Indonesia"
        And I should see translation button with language code "EN"

    @featureToggle
    Scenario: Note detail view switch Back to English
        Given I switch language to "ID"
        When I switch language to "EN"
        Then Note title will be shown "English"
        And Note description will be shown "English Language"
        And I should see translation button with language code "ID"

    @featureToggle
    Scenario Outline: Cards and Article view have language same with parent
        Given I switch language to "ID"
        When I jump to "<View>" tab
        Then Note title will be shown "<Title>"
        And Note description will be shown "<Description>"
        And I should see translation button with language code "<CurrentLang>"

        Examples:
            | View | Title | Description | CurrentLang |
            | article view  | Indonesia  | Bahasa Indonesia  | EN |
            | cards view  | Indonesia  | Bahasa Indonesia  | EN |

    @featureToggle
    Scenario: View translation on article view in English
        Given I jump to "article view" tab
        Then Note title will be shown "English"
        And Note description will be shown "English Language"
        And I should see translation button with language code "ID"

    @featureToggle
    Scenario: Switch to Indonesia on article view
        Given I jump to "article view" tab
        When I switch language to "ID"
        Then Note title will be shown "Indonesia"
        And Note description will be shown "Bahasa Indonesia"
        And I should see translation button with language code "EN"