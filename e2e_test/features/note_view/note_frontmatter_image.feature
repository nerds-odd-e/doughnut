Feature: Note header image from frontmatter
  As a learner, I want my note header image to follow the image property in YAML frontmatter

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Frontmatter images" with a note "shown" and content "placeholder"

  Scenario: Note show displays image when content includes image frontmatter
    When I update note "shown" content using markdown to become:
      """
      ---
      image: /attachments/images/42/e2e-frontmatter.png
      ---
      Body text
      """
    And I flush pending note content save
    When I should see note "shown" has an image
