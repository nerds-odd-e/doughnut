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
