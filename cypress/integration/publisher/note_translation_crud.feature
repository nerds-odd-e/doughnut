Feature: Note Translation Crud
    As a user, I want to be able to insert or update my translation of title and description of a note through a form popup.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
        | title          | testingParent | description          |
        | English        |               | English Language     |
        And I click on the overview button of note "English"

    @featureToggle
    Scenario: Should able to edit note translation
        When I edit note translation to become
      | Title in Indonesian     | Description in Indonesian       |
      | Indonesia               | Bahasa Indonesia                |
        And I switch language to "ID"
        And Note title will be shown "Indonesia"
        And Note description will be shown "Bahasa Indonesia"
        And I should see translation button with language code "EN"