Feature: EPUB book

  Scenario: Upload supported EPUB and see book name on reading page
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Attach E2E Notebook"
    When I open the notebook settings for "EPUB Attach E2E Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    Then I should see the EPUB reading view with book name "epub_valid_minimal"
    And I should see the book layout in the browser:
      | 0 | Part One          |
      | 1 | Chapter Alpha     |
      | 0 | Chapter Beta      |
      | 1 | Section Beta-One  |
      | 1 | Section Beta-Two  |

  Scenario: Read EPUB content and navigate to a chapter
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Reading Notebook"
    When I open the notebook settings for "EPUB Reading Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    Then I should see the text "Opening paragraph for part one." in the EPUB reader
    When I click "Chapter Beta" in the book layout
    Then I should see the text "Cell One" in the EPUB reader

  Scenario: EPUB current block updates on scroll; selection stays on explicit choice
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Current vs Selected Notebook"
    When I open the notebook settings for "EPUB Current vs Selected Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    When I click "Chapter Alpha" in the book layout
    Then the book block "Chapter Alpha" should be the current selection in the book reader
    And the book block "Chapter Alpha" should be the current block in the book reader
    When I scroll the EPUB reader until the text "Cell One" is in the viewport
    Then the book block "Chapter Alpha" should be the current selection in the book reader
    And the current block in the book layout should not be the selected block

  Scenario: Navigate precisely to a sub-section within an EPUB chapter
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Precise Nav Notebook"
    When I open the notebook settings for "EPUB Precise Nav Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    Then the book layout block "Section Beta-Two" should have epub start href containing "#section-beta-two"
    When I click "Section Beta-Two" in the book layout
    Then I should see the text "Unique content in section beta-two." in the EPUB reader

  Scenario: Resume EPUB at the last read position after leaving
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Resume Notebook"
    When I open the notebook settings for "EPUB Resume Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    And I click "Section Beta-Two" in the book layout
    Then I should see the text "Unique content in section beta-two." in the EPUB reader
    When I leave the EPUB reading view and return to it
    Then I should see the text "Unique content in section beta-two." in the EPUB reader
    And the book block "Section Beta-Two" should be the current block in the book reader

  Scenario: Entering the next EPUB block auto-marks a structural-only predecessor as read
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Auto-Mark Predecessor Notebook"
    When I open the notebook settings for "EPUB Auto-Mark Predecessor Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    Then I should see the text "Opening paragraph for part one." in the EPUB reader
    When I click "Chapter Alpha" in the book layout
    Then I should see the text "Body text with an illustration." in the EPUB reader
    And I should see that book block "Part One" is marked as read in the book layout

  Scenario: Mark an EPUB block as read advances the selection
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Mark Read Notebook"
    When I open the notebook settings for "EPUB Mark Read Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    And I click "Chapter Alpha" in the book layout
    And I mark the book block "Chapter Alpha" as read in the Reading Control Panel
    Then I should see that book block "Chapter Alpha" is marked as read in the book layout
    And I should see that book block "Chapter Beta" is selected in the book layout

  Scenario: Mark an EPUB block as skimmed advances the selection
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Mark Skim Notebook"
    When I open the notebook settings for "EPUB Mark Skim Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    And I click "Chapter Alpha" in the book layout
    And I mark the book block "Chapter Alpha" as skimmed in the Reading Control Panel
    Then I should see that book block "Chapter Alpha" is marked as skimmed in the book layout
    And I should see that book block "Chapter Beta" is selected in the book layout

  Scenario: EPUB Reading Control Panel anchors below direct content for the selected block
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Panel Anchor Notebook"
    When I open the notebook settings for "EPUB Panel Anchor Notebook"
    And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
    When I open the reading view for the attached book "epub_valid_minimal"
    And I click "Chapter Alpha" in the book layout
    Then I should see the text "Body text with an illustration." in the EPUB reader
    And the EPUB Reading Control Panel should be content-anchored

  Scenario: Upload DRM-flagged EPUB shows a clear attach error
    Given I am logged in as an existing user
    And I have a notebook with the head note "EPUB Unsupported Attach E2E Notebook"
    When I open the notebook settings for "EPUB Unsupported Attach E2E Notebook"
    And I attempt to attach the EPUB file "book_reading/epub_invalid_drm_encryption_xml.epub"
    Then I should see an EPUB attach error containing "encrypted or DRM-protected"
