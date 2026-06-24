@disableOpenAiService
Feature: Title alias migration
  As an admin, I want inbound wiki links rewritten after title aliases move to frontmatter
  so that [[alias]] links resolve and recall keeps working.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Alias migration" with notes:
      | Title         | Content                                       |
      | colour／color | Non-empty body for recall                     |
      | link referrer | See [[colour／color]]. Alias link [[color]]. |

  Scenario: Migration rewrites legacy full-title links and alias recall keeps working
    Given I am re-logged in as an admin
    When I run the admin data migration to completion
    And I am re-logged in as "old_learner"
    And injected note "colour／color" is indexed as "colour"
    And I route to the note "link referrer"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[colour]]"
    And the note content markdown source should contain "[[color]]"
    And the note content markdown source should not contain "[[colour／color]]"
    When I am assimilating the note "colour"
    And I assimilate with remembering spelling
    When I verify spelling with "color"
    Then the spelling verification result for note "colour" should be "success"
