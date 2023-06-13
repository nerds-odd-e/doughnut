@ignore
Feature: Question generation

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title        | description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
      | Scuba Diving | Scuba diving is an exhilarating and rewarding activity that allows you to explore the underwater world and witness firsthand the stunning beauty of marine life. However, before you can dive, it's important to obtain the necessary licenses. The most common scuba diving certification is the Open Water Diver certification, which allows you to dive up to a depth of 60 feet (18 meters) with a buddy. To obtain this certification, you'll need to complete a scuba diving course that covers the basics of diving, including equipment use, safety procedures, and underwater communication. |

  @usingMockedOpenAiService
  Scenario: AI will generate a question
    Given OpenAI returns a question
      | question                                            | option_a                   | option_b                 | option_c                       |
      | What is the most common scuba diving certification? | Rescue Diver certification | Divemaster certification | Open Water Diver certification |
    When I ask to generate a question for note "Scuba Diving"
    Then I should see a question on current page
      | question                                            | option_a                   | option_b                 | option_c                       |
      | What is the most common scuba diving certification? | Rescue Diver certification | Divemaster certification | Open Water Diver certification |
