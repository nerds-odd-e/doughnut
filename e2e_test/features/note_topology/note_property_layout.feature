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
