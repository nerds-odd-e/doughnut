@ignore
@withCliConfig
@interactiveCLI
@disableOpenAiService
Feature: Push a local workspace into a notebook

  As a notebook owner
  I want to push the notes I edited locally back into Doughnut
  So that the workspace I read and write in is where I can work

  A push updates notes the notebook already has, matched by their path in the
  workspace. The frontmatter of a local file is the note's properties, so a push
  carries them up with the body.

  A push remembers the workspace it was given, so the next one can leave the
  path out.

  Background:
    Given I am logged in as an existing user
    And I set the access token for "old_learner" in the interactive CLI
    And I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | less  | Hello   |
    And I enter the slash command "/use Ben Notebook" in the interactive CLI

  Scenario: A body edited locally reaches Doughnut
    Given the workspace "./BenNotebook" holds the same content as "Ben Notebook"
    When the file "less.md" in the workspace "./BenNotebook" is:
      """
      # less

      Hello from Obsidian
      """
    And I enter the slash command "/push ./BenNotebook" in the interactive CLI
    Then I should see "1 note updated." in past CLI assistant messages
    And the note "less" in Doughnut should hold "Hello from Obsidian"

  Scenario: A property edited locally reaches Doughnut
    Given the note "less" in Doughnut has property "url" as "http://old"
    And the workspace "./BenNotebook" holds the same content as "Ben Notebook"
    When the file "less.md" in the workspace "./BenNotebook" is:
      """
      ---
      url: http://new
      ---

      # less

      Hello
      """
    And I enter the slash command "/push ./BenNotebook" in the interactive CLI
    Then I should see "1 note updated." in past CLI assistant messages
    And the note "less" in Doughnut should have property "url" as "http://new"

  Scenario: A second push goes to the workspace the first one named
    Given the workspace "./BenNotebook" holds the same content as "Ben Notebook"
    And I enter the slash command "/push ./BenNotebook" in the interactive CLI
    When the file "less.md" in the workspace "./BenNotebook" is:
      """
      # less

      Hello from Obsidian
      """
    And I enter the slash command "/push" in the interactive CLI
    Then I should see "1 note updated." in past CLI assistant messages
    And the note "less" in Doughnut should hold "Hello from Obsidian"
