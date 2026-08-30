Feature: Property wiki links
  As a learner, I want a wiki link to a property so I can open that property
  panel from note content or a property value.

  Background:
    Given I am logged in as an existing user

  Scenario: A live property wiki link opens the property panel
    Given I have a notebook "WikiProp Live NB"
    And I have a note "WikiProp Moon" under notebook "WikiProp Live NB" with content:
      """
      ---
      topic: luna
      ---

      Moon body.
      """
    And I have a note "WikiProp Carrier" under notebook "WikiProp Live NB" with content:
      """
      origin
      """
    When I update note "WikiProp Carrier" content using markdown to become:
      """
      See [[WikiProp Moon#prop:topic]].
      """
    Then I should see the note content rendered as:
      | Kind           | Text                     |
      | live wiki link | WikiProp Moon#prop:topic |
    And the wiki link "WikiProp Moon#prop:topic" should open property "topic" of note "WikiProp Moon"
    And the rich note property "topic" should be focused with its property panel open

  Scenario: A live property wiki in a property value opens the property panel
    Given I have a notebook "WikiProp Value NB"
    And I have a note "WikiProp Moon" under notebook "WikiProp Value NB" with content:
      """
      ---
      topic: luna
      ---

      Moon body.
      """
    And I have a note "WikiProp Carrier" under notebook "WikiProp Value NB" with content:
      """
      ---
      see: "[[WikiProp Moon#prop:topic]]"
      ---

      origin
      """
    When I visit note "WikiProp Carrier"
    Then the wiki link "WikiProp Moon#prop:topic" should open property "topic" of note "WikiProp Moon"
    And the rich note property "topic" should be focused with its property panel open

  Scenario: An unresolved property wiki does not navigate
    Given I have a notebook "WikiProp Dead NB"
    And I have a note "WikiProp Moon" under notebook "WikiProp Dead NB" with content:
      """
      Moon body.
      """
    And I have a note "WikiProp Carrier" under notebook "WikiProp Dead NB" with content:
      """
      origin
      """
    When I update note "WikiProp Carrier" content using markdown to become:
      """
      See [[WikiProp Moon#prop:topic]].
      """
    Then I should see the note content rendered as:
      | Kind           | Text                     |
      | dead wiki link | WikiProp Moon#prop:topic |
    When I follow the dead wiki link "WikiProp Moon#prop:topic"
    Then I should be at note "WikiProp Carrier"

  Scenario: Removing the target property makes a cached property wiki unresolved
    Given I have a notebook "WikiProp Stale Remove NB"
    And I have a note "WikiProp Moon" under notebook "WikiProp Stale Remove NB" with content:
      """
      ---
      topic: luna
      ---

      Moon body.
      """
    And I have a note "WikiProp Carrier" under notebook "WikiProp Stale Remove NB" with content:
      """
      See [[WikiProp Moon#prop:topic]].
      """
    When I update note "WikiProp Moon" content using markdown to become:
      """
      Moon body.
      """
    And I visit note "WikiProp Carrier"
    Then I should see the note content rendered as:
      | Kind           | Text                     |
      | dead wiki link | WikiProp Moon#prop:topic |
    When I follow the dead wiki link "WikiProp Moon#prop:topic"
    Then I should be at note "WikiProp Carrier"

  Scenario: Renaming the target property makes a cached property wiki unresolved
    Given I have a notebook "WikiProp Stale Rename NB"
    And I have a note "WikiProp Moon" under notebook "WikiProp Stale Rename NB" with content:
      """
      ---
      topic: luna
      ---

      Moon body.
      """
    And I have a note "WikiProp Carrier" under notebook "WikiProp Stale Rename NB" with content:
      """
      See [[WikiProp Moon#prop:topic]].
      """
    When I update note "WikiProp Moon" content using markdown to become:
      """
      ---
      subject: luna
      ---

      Moon body.
      """
    And I visit note "WikiProp Carrier"
    Then I should see the note content rendered as:
      | Kind           | Text                     |
      | dead wiki link | WikiProp Moon#prop:topic |
    When I follow the dead wiki link "WikiProp Moon#prop:topic"
    Then I should be at note "WikiProp Carrier"

  Scenario: Removing a self-targeted property makes a cached property wiki unresolved
    Given I have a notebook "WikiProp Stale Self NB"
    And I have a note "WikiProp Moon" under notebook "WikiProp Stale Self NB" with content:
      """
      ---
      topic: luna
      ---

      See [[WikiProp Moon#prop:topic]].
      """
    When I update note "WikiProp Moon" content using markdown to become:
      """
      See [[WikiProp Moon#prop:topic]].
      """
    Then I should see the note content rendered as:
      | Kind           | Text                     |
      | dead wiki link | WikiProp Moon#prop:topic |
    When I follow the dead wiki link "WikiProp Moon#prop:topic"
    Then I should be at note "WikiProp Moon"

  Scenario: Renaming a referenced note keeps the property wiki suffix and display text
    Given I have a notebook "WikiProp Rename NB"
    And I have a note "WikiProp Moon" under notebook "WikiProp Rename NB" with content:
      """
      ---
      topic: luna
      ---

      Moon body.
      """
    And I have a note "WikiProp Carrier" under notebook "WikiProp Rename NB" with content:
      """
      See [[WikiProp Moon#prop:topic|shown]].
      """
    When I route to the note "WikiProp Moon"
    And I set the note title to "WikiProp Luna" keeping visible reference text
    And I route to the note "WikiProp Carrier"
    Then the wiki link "shown" should open property "topic" of note "WikiProp Luna"
    And the rich note property "topic" should be focused with its property panel open

  @mockBrowserTime
  Scenario: Moving a referenced note across notebooks keeps the property wiki suffix and display text
    Given I have a notebook "WikiProp Move Old NB"
    And I have a note "WikiProp Moon" under notebook "WikiProp Move Old NB" with content:
      """
      ---
      topic: luna
      ---

      Moon body.
      """
    And I have a note "WikiProp Carrier" under notebook "WikiProp Move Old NB" with content:
      """
      See [[WikiProp Moon#prop:topic|shown]].
      """
    And I have a notebook "WikiProp Move New NB"
    When I route to the note "WikiProp Moon"
    And I move the current note to notebook "WikiProp Move New NB" root
    And I route to the note "WikiProp Carrier"
    Then the wiki link "shown" should open property "topic" of note "WikiProp Moon"
    And the rich note property "topic" should be focused with its property panel open
