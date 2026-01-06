@ignore
Feature: Remove points from note when assimilating

Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" and details "English serves as the world's most widespread lingua franca, vital for global communication, connecting people across cultures and nations. It's the standard language for international business, science, technology, and diplomacy, significantly enhancing career opportunities. English dominates the internet and digital world, with most online content in the language. It's the language of global media, including major films, music, and entertainment, granting access to diverse cultures. Proficiency in English simplifies international travel, allowing for easier navigation and connection with locals. It's an official language in over 60 countries and a primary foreign language in many others, making it essential for academic pursuits and global participation."

Scenario: Remove points from note when assimilating
    Given OpenAI generates understanding checklist with points:
        | English is the world's most widespread lingua franca |
        | It's vital for global communication and connects people across cultures |
        | English is the standard language for international business, science, and diplomacy |
        | English dominates the internet and digital world |
        | Proficiency in English enhances career opportunities and simplifies international travel |
    When I start assimilating "English"
    And I mark the point "English is the world's most widespread lingua franca" for removal as unrelated to the note
    And OpenAI create a new understanding checklist with points
    Then I should see an understanding checklist with 5 points:
        | It's vital for global communication and connects people across cultures |
        | English is the standard language for international business, science, and diplomacy |
        | English dominates the internet and digital world |
        | Proficiency in English enhances career opportunities and simplifies international travel |    
        | The mouse is the most beautiful creature in the world |
    And I should see the point "English is the world's most widespread lingua franca" is removed from the note