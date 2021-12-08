Feature: Synchronize Translation
  As a book editor I want to see outdated tag on Indonesian translation after I edit the English translation note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | titleIDN  | description      | descriptionIDN   |
      | English | Indonesia | English Language | Bahasa Indonesia |
    And I open the "article" view of note "English"

  @ignore
  @featureToggle
  Scenario: Should display outdated tag on Indonesian translation
    When I edit english note translation to become
      | Title   |
      | Inggris |
    And Note title on the page should be "Inggris"
    And I switch language to "ID"
    Then I should see outdated tag