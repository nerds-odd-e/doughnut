Feature: EPUB book

  Scenario: Upload supported EPUB and see book name on reading page
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Attach E2E Notebook"
    When I open the notebook settings for "EPUB Attach E2E Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    Then I should see the EPUB reading view with book name "epub_valid_minimal"
