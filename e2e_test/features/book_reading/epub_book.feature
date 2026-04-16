Feature: EPUB book

  Scenario: Upload supported EPUB and see book name on reading page
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Attach E2E Notebook"
    When I open the notebook settings for "EPUB Attach E2E Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    Then I should see the EPUB reading view with book name "epub_valid_minimal"
    And I should see the book layout in the browser:
      | 0 | Part One        |
      | 1 | Chapter Alpha   |
      | 0 | Chapter Beta    |

  Scenario: Read EPUB content and navigate to a chapter
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Reading Notebook"
    When I open the notebook settings for "EPUB Reading Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    Then I should see the text "Opening paragraph for part one." in the EPUB reader
    When I click "Chapter Beta" in the book layout
    Then I should see the text "Cell One" in the EPUB reader

  Scenario: Upload DRM-flagged EPUB shows a clear attach error
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Unsupported Attach E2E Notebook"
    When I open the notebook settings for "EPUB Unsupported Attach E2E Notebook"
    And I attempt to attach the EPUB file "book_reading/epub_invalid_drm_encryption_xml.epub"
    Then I should see an EPUB attach error containing "encrypted or DRM-protected"
