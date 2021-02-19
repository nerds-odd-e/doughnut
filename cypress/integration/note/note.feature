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
    Given I have some notes
    | note-title      |   note-description  | note-createdDateTime     |
    | Sedition        |   Incite violence   | 2021-02-19T02:42:07.597Z |
    | Sedation        |   Put to sleep      | 2021-02-19T02:42:08.597Z |
    When I review my notes
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
    | note-title      |   note-description  | note-createdDateTime     |
    | A               |   This ia a A       | 2021-02-19T02:42:07.597Z |
    | B               |   This is a B       | 2021-02-19T02:42:08.597Z |
    When I review my notes
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | B               |   This is a B      |

    
@clean_db @login_as_new_user @ignore
Scenario: New user review note with link
    When I link Sedition to Sedation
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
