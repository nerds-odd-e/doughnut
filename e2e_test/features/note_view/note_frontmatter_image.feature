Feature: Note header image from frontmatter
  As a learner, I want my note header image to follow the image property in YAML frontmatter

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Frontmatter images" with a note "shown" and content "placeholder"

  Scenario: Note show displays image when content includes image frontmatter
    When I update note "shown" content using markdown to become:
      """
      ---
      image: https://example.com/a.png
      ---
      Body text
      """
    When I should see note "shown" has an image

  Scenario: Rich editor uploads image property with attachment path in frontmatter
    When I upload an image from fixture "moon.jpg" to the note "shown"
    And I reload the current page for note "shown"
    Then the rich note property "image" should show an attachment image path
    When I open the note content markdown editor
    Then the note content markdown source should contain "image: /attachments/images/"

  Scenario: Rich editor sets image property from URL in frontmatter
    When I set rich note image property URL "https://example.com/a.png" on note "shown"
    And I reload the current page for note "shown"
    Then I should see note "shown" has an image
    And I should see rich note property "image" with value "https://example.com/a.png"
