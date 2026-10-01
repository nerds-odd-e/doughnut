Feature: Note property layout
  As a learner on an iPad or a phone, I want every property value of a note to stay visible
  so that a long key or value does not hide what I need to read.

  Background:
    Given I am re-logged in as "another_old_learner"
    And I have a notebook "Long property shelf"
    And I have a note "Long properties" under notebook "Long property shelf" with content:
      """
      ---
      a_rather_long_property_key_name_that_keeps_going_and_going_without_any_space: short
      a rather long property key name that keeps going and going with spaces in it: This value is deliberately long so that it must wrap onto several lines instead of being cut off or hidden. This value is deliberately long so that it must wrap onto several lines instead of being cut
      ---

      Long properties body.
      """
    And notebook "Long property shelf" is shared to the Bazaar
    And I am re-logged in as "old_learner"
    And I have subscribed to notebook "Long property shelf" in the bazaar with daily assimilation target of 1

  Scenario Outline: A read-only property with a long key keeps its value visible
    Given I am on a window <width> * 1000
    When I visit note "Long properties"
    Then the value of property "a_rather_long_property_key_name_that_keeps_going_and_going_without_any_space" should be visible
    And the value of property "a rather long property key name that keeps going and going with spaces in it" should be visible
    And the note should not scroll sideways

    Examples:
      | width |
      | 820   |
      | 375   |

  Scenario Outline: An editable property with a long key keeps its value visible and its controls apart
    Given I am re-logged in as "another_old_learner"
    And I am on a window <width> * 1000
    When I visit note "Long properties"
    And I open the property panel for property "a_rather_long_property_key_name_that_keeps_going_and_going_without_any_space"
    And I open the property panel for property "a rather long property key name that keeps going and going with spaces in it"
    Then the editable value of property "a rather long property key name that keeps going and going with spaces in it" should show all its text
    And the key of property "a_rather_long_property_key_name_that_keeps_going_and_going_without_any_space" should end with an ellipsis
    And the controls of property "a rather long property key name that keeps going and going with spaces in it" should not overlap
    And the note should not scroll sideways

    Examples:
      | width |
      | 820   |
      | 375   |

  Scenario: A touch device reports a coarse pointer
    Given I use a touch device
    When I visit note "Long properties"
    Then the device should report a coarse pointer

  Scenario: A touch device gets property controls of at least 44 px
    Given I am re-logged in as "another_old_learner"
    And I have a note "Linked" under notebook "Long property shelf" with content:
      """
      ---
      url: https://example.com/a/b
      ---

      Linked body.
      """
    And I use a touch device
    And I am on a window 820 * 1000
    When I visit note "Linked"
    And I open the property panel for property "url"
    Then the controls of property "url" should be at least 44 px high

  Scenario: A touch device gets new property controls of at least 44 px
    Given I am re-logged in as "another_old_learner"
    And I use a touch device
    And I am on a window 820 * 1000
    When I visit note "Long properties"
    And I start adding a property with key "topic"
    Then the new property Add and Cancel controls should be at least 44 px high

  Scenario: Without a touch device the property controls stay compact
    Given I am re-logged in as "another_old_learner"
    And I have a note "Linked" under notebook "Long property shelf" with content:
      """
      ---
      url: https://a.io
      ---

      Linked body.
      """
    And I am on a window 820 * 1000
    When I visit note "Linked"
    And I open the property panel for property "url"
    Then the controls of property "url" should be under 44 px high

  Scenario Outline: The key presets of a new property do not cover the value field
    Given I am re-logged in as "another_old_learner"
    And I am on a window <width> * 1000
    When I visit note "Long properties"
    And I start adding a property with key "url"
    Then the new property value should not be covered
    When I start adding a property with key "image"
    Then the new property Choose image button should not be covered

    Examples:
      | width |
      | 375   |

  Scenario Outline: A long key preset wraps inside the key panel of a new property
    Given I am re-logged in as "another_old_learner"
    And I am on a window <width> * 1000
    When I visit note "Long properties"
    And I start adding a property with key "question"
    Then the key preset "question_generation_instruction" should stay inside the key panel
    And the new property value should not be covered

    Examples:
      | width |
      | 820   |
      | 375   |

  Scenario: A long key preset wraps inside the key panel of an existing property
    Given I am re-logged in as "another_old_learner"
    And I am on a window 820 * 1000
    When I visit note "Long properties"
    And I type "question" in the key of property "a_rather_long_property_key_name_that_keeps_going_and_going_without_any_space"
    Then the key preset "question_generation_instruction" should stay inside the key panel
