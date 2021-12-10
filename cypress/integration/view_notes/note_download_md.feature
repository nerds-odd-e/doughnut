Feature: Download Note to .md file
    As a user, I want to be able to download note that currently open as .md file.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description  | pictureUrl    | url | 
      | Singapore | Singapore is great | https://www.gstatic.com/webp/gallery/1.jpg  | http://localhost:3000/ |
    And I open the "article" view of note "Singapore"

  @featureToggle  @cleanDownloadFolder
  Scenario: User download current note
    Then I download note
    Then There is a "Singapore.md" file downloaded
    And the file "Singapore.md" content is
    """md
    # Singapore
    Singapore is great
    ![image](https://www.gstatic.com/webp/gallery/1.jpg)
    [url](http://localhost:3000/)
    """
