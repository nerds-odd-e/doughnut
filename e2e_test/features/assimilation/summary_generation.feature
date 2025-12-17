Feature: Summary Generation
    As a learner, I want to generate a summary of my note using AI

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" and details "English serves as the world's most widespread lingua franca, vital for global communication, connecting people across cultures and nations. It's the standard language for international business, science, technology, and diplomacy, significantly enhancing career opportunities. English dominates the internet and digital world, with most online content in the language. It's the language of global media, including major films, music, and entertainment, granting access to diverse cultures. Proficiency in English simplifies international travel, allowing for easier navigation and connection with locals. It's an official language in over 60 countries and a primary foreign language in many others, making it essential for academic pursuits and global participation."
    When I start assimilating "English"

  Scenario: Generate a summary of a note
    Then I should see a summary of the note broken down into a maximum of 5 points

  Scenario: Exclude summary generation for note type "Initiative"
    And the note type is "initiative"
    Then I should see a text "No summary requested for initiative notes." in the summary section