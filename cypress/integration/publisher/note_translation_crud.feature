Feature: Note Translation Crud
    As a user, I want to be able to insert or update my translation of title and description of a note through a form popup.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
        | title          | testingParent | description          |
        | English        |               | English Language     |
        And I open the "article" view of note "English"

    @featureToggle
    Scenario: Should able to edit note translation
        When I edit note translation to become
      | Title in Indonesian     | Description in Indonesian       |
      | Indonesia               | Bahasa Indonesia                |
        And Note title on the page should be "Indonesia"
        And Note description on the page should be "Bahasa Indonesia"