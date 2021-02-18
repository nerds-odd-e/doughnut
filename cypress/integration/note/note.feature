Feature: Note related feature

@clean_db
Scenario: New user create a note without login
    When I did not log in
    And I create note
    Then I should be asked to log in

@clean_db @login_as_new_user 
Scenario: New user create notes after login
    When I create note with: 
    | note-title      |   note-description  |
    | Sedation        |   Put to sleep      |
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | Sedation        |   Put to sleep      |

@clean_db @login_as_new_user
Scenario: New user review multiple notes
    When I create note with: 
    | note-title      |   note-description  |
    | Sedition        |   Incite violence   |
    | Sedation        |   Put to sleep      |
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | Sedation        |   Put to sleep      |
    And I click on next note
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | Sedition        |   Incite violence   |

@clean_db @login_as_new_user
Scenario: : Get a list of notes
    Given I have some notes
    When I review my notes
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | Sedation        |   Put to sleep      |

    