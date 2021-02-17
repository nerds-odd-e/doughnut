Feature: link note

#  @ignore
#  Scenario: Link two notes together
#    Given I have created two notes
#      | title      |
#      | Sedition   |
#      | Sedation   |
#    When I select the note Sedition
#    And I create link
#    And I select Sedation note as target
#    Then I should see the link created between note Sedition and Sedation

  Background:
      Given I create note with:
      | note-title      |  note-description       |
      | Sedition        |   Incite violence   |
      | Sedation        |   Put to sleep      |
      | Sedative        |   sleep medicine    |

@ignore @clean_db @login_as_new_user @link_note
    Scenario Outline: View all linkable notes when no links exist
        When I navigate to the notes page
        Then I should see 3 notes belonging to the user
            | note-title       |
            | Sedition        |
            | Sedation        |
            | Sedative        |
        When I click Create Link button on:
            | note-title      |
            | Sedition        |
        Then I should be navigated to the linking page
        And I should see below notes

        Examples:
        | note-title      |   note-description       |
        | Sedation        |   Put to sleep      |

@ignore @clean_db @login_as_new_user
    Scenario Outline: View all linkable notes when there are existing links
        Given link exist form Sedition to sedation
        When I navigate to the notes page
        Then I should see all notes belonging to the user
        When I click Create Link button on Sedition note card
        Then I should be navigated to the linking page
        And I should see below notes

        Examples:
        | note-title      |   note-description       |
        | Sedative        |   sleep medicine    |