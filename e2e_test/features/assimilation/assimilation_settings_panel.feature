Feature: Assimilation settings panel on note page
  As a learner, I want to open assimilation settings on the normal note page
  without navigating to a separate assimilate route.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Deck" with notes:
      | Title | Content       |
      | Alpha | Alpha content |

  Scenario: Keep for recall from inline assimilation settings
    When I jump to the note page of "Alpha"
    And I open assimilation settings from more options
    When I keep for recall on the assimilation panel
    Then the keep for recall button should be disabled
