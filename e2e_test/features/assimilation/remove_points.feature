@ignore
Feature: Remove points from note when assimilating

Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" and details "English serves as the world's most widespread lingua franca, vital for global communication, connecting people across cultures and nations. It's the standard language for international business, science, technology, and diplomacy, significantly enhancing career opportunities. English dominates the internet and digital world, with most online content in the language. It's the language of global media, including major films, music, and entertainment, granting access to diverse cultures. Proficiency in English simplifies international travel, allowing for easier navigation and connection with locals. It's an official language in over 60 countries and a primary foreign language in many others, making it essential for academic pursuits and global participation."

  @usingMockedOpenAiService
  Scenario: Remove points from note when assimilating
    Given OpenAI generates understanding checklist with points:
      | English is the world's most widespread lingua franca |
      | It's vital for global communication and connects people across cultures |
      | English is the standard language for international business, science, and diplomacy |
      | English dominates the internet and digital world |
      | Proficiency in English enhances career opportunities and simplifies international travel |
    When I start assimilating "English"
    And I mark the point "English is the world's most widespread lingua franca" for removal as unrelated to the note
    Then I should the note rephrased as "English plays a crucial role in global communication, enabling people from different cultures and nations to understand one another. It is the primary language used in international business, science, technology, and diplomacy, which significantly broadens professional and academic opportunities. The language is deeply embedded in the digital world, with a large majority of online content created in English. Global media—such as films, music, and entertainment—relies heavily on English, offering access to a wide range of cultures and ideas. Knowledge of English also makes international travel easier, helping people navigate foreign environments and communicate with locals more effectively. As an official language in over 60 countries and a widely taught foreign language elsewhere, English is essential for global participation and education."
    And OpenAI generates the understanding checklist with points:
      | A | English enables effective communication between people from different cultures and countries |
      | B | It is the main language of international business, science, technology, and diplomacy |
      | C | English provides strong advantages for careers and academic pursuits |
      | D | The majority of digital and online content is available in English |
      | E | Proficiency in English facilitates international travel and access to global media |
