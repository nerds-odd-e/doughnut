Feature: EPUB book

  Rule: Supported minimal EPUB in reading view

    Background:
      Given I am logged in as an existing user
      And I have a notebook "EPUB smoke" with a note "EPUB E2E Notebook"
      And I open the notebook settings for "EPUB smoke"
      And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
      And I open the reading view for the attached book "epub_valid_minimal"

    Scenario: See EPUB structure and opening content
      Then I should see the EPUB reading view with book name "epub_valid_minimal"
      And I should see the book layout in the browser:
        | 0 | Part One          |
        | 1 | Chapter Alpha     |
        | 0 | Chapter Beta      |
        | 1 | Section Beta-One  |
        | 1 | Section Beta-Two  |
      And I should see the text "Opening paragraph for part one." in the EPUB reader

    Scenario: Navigate the EPUB via the book layout
      When I choose the book block "Chapter Beta"
      Then I should see the text "Cell One" in the EPUB reader
      And the book layout block "Section Beta-Two" should have epub start href containing "#section-beta-two"
      When I choose the book block "Section Beta-Two"
      Then I should see the text "Unique content in section beta-two." in the EPUB reader

    Scenario: Resume EPUB reading at the last position
      When I choose the book block "Section Beta-Two"
      Then I should see the text "Unique content in section beta-two." in the EPUB reader
      When I leave the EPUB reading view and return to it
      Then I should see the text "Unique content in section beta-two." in the EPUB reader

    Scenario: EPUB reading control panel is content-anchored
      When I choose the book block "Chapter Alpha"
      Then I should see the text "Body text with an illustration." in the EPUB reader
      And the EPUB Reading Control Panel should be content-anchored

    Scenario: EPUB reading resumes at the scrolled fragment, not the inferred block start
      When I choose the book block "Chapter Beta"
      And I scroll the EPUB reader host to the top
      Then I should see the text "Chapter Beta" in the EPUB reader
      When I scroll the EPUB reader until the text "Cell One" is in the viewport
      And I leave the EPUB reading view and return to it
      Then I should see the text "Cell One" in the EPUB reader

    Scenario: Current block updates on scroll while explicit book layout selection is unchanged
      When I choose the book block "Chapter Alpha"
      Then the book block "Chapter Alpha" should be the current selection in the book reader
      And the book block "Chapter Alpha" should be the current block in the book reader
      When I scroll the EPUB reader until the heading "Chapter Beta" passes the top
      Then the book block "Chapter Alpha" should be the current selection in the book reader
      And the current block in the book layout should not be the selected block

    Scenario: Entering the next EPUB block auto-marks a structural-only predecessor as read
      Then I should see the text "Opening paragraph for part one." in the EPUB reader
      When I choose the book block "Chapter Alpha"
      Then I should see the text "Body text with an illustration." in the EPUB reader
      And I should see that book block "Part One" is marked as read in the book layout

    Scenario: Mark an EPUB block as read advances the selection
      When I choose the book block "Chapter Alpha"
      And I mark the book block "Chapter Alpha" as read in the Reading Control Panel
      Then I should see that book block "Chapter Alpha" is marked as read in the book layout
      And I should see that book block "Chapter Beta" is selected in the book layout

    Scenario: Mark an EPUB block as skimmed advances the selection
      When I choose the book block "Chapter Alpha"
      And I mark the book block "Chapter Alpha" as skimmed in the Reading Control Panel
      Then I should see that book block "Chapter Alpha" is marked as skimmed in the book layout
      And I should see that book block "Chapter Beta" is selected in the book layout

  Rule: Landing on the chosen place

    Background:
      Given I am logged in as an existing user
      And I have a notebook "EPUB landing" with a note "EPUB Landing E2E Notebook"
      And I open the notebook settings for "EPUB landing"
      And I attach the EPUB file "book_reading/epub_long_chapter_before_target.epub"

    Scenario Outline: Choosing a chapter after a long chapter shows its heading at the top
      Given I set the book reading viewport to <width> by <height>
      And I open the reading view for the attached book "epub_long_chapter_before_target"
      When I choose the book block "Chapter Two"
      Then the heading "Chapter Two" should be at the top of the EPUB reader
      And the book block "Chapter Two" should be the current block in the book reader

      Examples:
        | width | height |
        | 1440  | 900    |
        | 1280  | 560    |

    Scenario: Opening a new EPUB for the first time shows its cover as the current block and marks nothing
      When I open the reading view for the attached book "epub_long_chapter_before_target"
      Then I should see the text "A cover page for the long chapter fixture." in the EPUB reader
      And the book block "*beginning*" should be the current block in the book reader
      And no book block should be marked in the book layout

    Scenario: Following a link inside the book shows its heading at the top
      Given I set the book reading viewport to 1280 by 560
      And I open the reading view for the attached book "epub_long_chapter_before_target"
      When I follow the link "Go to Chapter Two" in the EPUB reader
      Then the heading "Chapter Two" should be at the top of the EPUB reader
      And the book block "Chapter Two" should be the current block in the book reader

    Scenario: Choosing a block with no content of its own shows its table-of-contents target
      Given I open the reading view for the attached book "epub_long_chapter_before_target"
      When I choose the book block "The Full Licence"
      Then the heading "The Full Licence" should be at the top of the EPUB reader
      And the book block "The Full Licence" should be the current selection in the book reader
      And the book block "The Full Licence" should be the current block in the book reader

    Scenario: Choosing the first of two blocks sharing a start keeps it selected and current
      Given I open the reading view for the attached book "epub_long_chapter_before_target"
      When I choose the book block "The Fixture Title"
      Then the heading "The Fixture Title" should be at the top of the EPUB reader
      And the book block "The Fixture Title" should be the current selection in the book reader
      And the book block "The Fixture Title" should be the current block in the book reader
      When I mark the book block "The Fixture Title" as read in the Reading Control Panel
      Then I should see that book block "The Fixture Title" is marked as read in the book layout

  Rule: Unsupported EPUB attachment

    Scenario: Upload DRM-flagged EPUB shows a clear attach error
      Given I am logged in as an existing user
      And I have a notebook "EPUB attach edge" with a note "EPUB Unsupported Attach E2E Notebook"
      When I open the notebook settings for "EPUB attach edge"
      And I attempt to attach the EPUB file "book_reading/epub_invalid_drm_encryption_xml.epub"
      Then I should see an EPUB attach error containing "encrypted or DRM-protected"
