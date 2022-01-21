Feature: Synchronize Translation
  As a book editor I want to see outdated tag on Indonesian translation after I edit the English translation note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | titleIDN | description      | descriptionIDN |
      | Pandava | Pandawa  | The five heroes. | Lima pahlawan  |
    And I open the "article" view of note "Pandava"

  @featureToggle
  Scenario: Should not change anything if translation is still up to date
    When I edit note translation to become
      | Title       |
      | putra Pandu |
    And Note title on the page should be "putra Pandu"
    Then I should not see translation outdated tag

  @featureToggle
  Scenario: Should inform me if outdated translation already updated
    When I edit original note translation to become
      | Title         |
      | sons of Pandu |
    And I switch language to "ID"
    Then I should see translation outdated tag
    And I switch language to "EN"
    When I edit note translation to become
      | Title       |
      | putra Pandu |
    And Note title on the page should be "putra Pandu"
    Then I should not see translation outdated tag