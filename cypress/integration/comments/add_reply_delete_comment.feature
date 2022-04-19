Feature: Add, Reply or Delete a comment
  As a reviewer, I want to comment on a note,
  so that I can exchange idea with the note owner.

  Background:
    Given I've logged in as "old_learner"

  @ignore
  @featureToggle
  Scenario: Comment on my own note
    And there are some notes for the current user
      | title        |
      | Less is More |
    When I comment with "please share more" on note "LeSS is More"
    Then I should see note "LeSS is More" has a comment "please share more" from "old_learner"

  @ignore
  @featureToggle
  Scenario: Comment and reply on note from same circle
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And  Someone seed a notebook "Team agreement" in circle "Odd-e SG Team"
    When I comment with "why is this empty?" on note "Team agreement"
    And I've logged in as "another_old_learner"
    Then I should see note "Team agreement" has a comment "why is this empty?" from "old_learner"
    When I reply with "used up our timebox" on note "Team agreement"
    Then there should be 2 comments for note "Team agreement"


  @ignore
  @featureToggle
  Scenario: Delete a comment
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And  there is a note "Team agreement" with comments "A" and "B" in circle "Odd-e SG Team" from the current user
    And I've logged in as "another_old_learner"
    When I delete the comment "A" for note "Team agreement"
    Then there should only be comment "B" for note "Team agreement"
