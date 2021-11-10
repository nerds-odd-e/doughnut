Feature: Note Translation Crud
    As a user, I want to be able to insert or update my translation of title and description of a note through a form popup.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
        | title            | testingParent | description |
        | English        |               | English Language     |
        And I click on the overview button of note "English"

    @featureToggle
    Scenario: Should show translation button
        When I click on button with text or title as "more options"
        Then I should see button with text or title as "edit note translation"

    @ignore
    @featureToggle
    Scenario: Should show translation edit popup
        When I click on button with text or title as "more options"
        And I should see button with text or title as "edit note translation"
        And I submit title translation as "Indonesia"
        Then I should see snackbar with title "Translation successfully saved"
        And I should see there are some notes for the current user with
        | title            | testingParent | description          |
        | English          |               | English Language     |