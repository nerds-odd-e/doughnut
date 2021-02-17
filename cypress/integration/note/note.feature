Feature: Note related feature

@clean_db
Scenario: New user create a note without login
    When I did not log in
    And I create note
    Then I should be asked to log in

@clean_db @login_as_new_user
Scenario: New user create a note after login
    When I create note with: 
    | note-title      |   note-description  |
    | Sedition        |   Incite violence   |
    | Sedation        |   Put to sleep      |
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | Sedation        |   Put to sleep      |
