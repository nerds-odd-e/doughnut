Feature: Note Translation Crud
    As a user, I want to be able to insert or update my translation of title and description of a note through a form popup.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
        | title          | testingParent | description          |
        | English        |               | English Language     |
        And I click on the overview button of note "English"

    @featureToggle
    Scenario: Should show translation button
        When I click on button with text or title as "more options"
        Then I should see button with text or title as "edit note translation"

    @featureToggle
    Scenario: Should able to edit note translation
        When I edit note translation to become
      | Title in Indonesian     | Description in Indonesian       |
      | Indonesia               | Bahasa Indonesia                |
        Then I should see confirmation title "Translation successfully saved"