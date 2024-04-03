 Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent  | details             |
      | FourK            |                | I'm testing.        |

  Scenario Outline: Attach audio file
    When I attach audio file "<audio_file>" to my note
    Then I should see "<audio_file>" in my note

    Examples:
      | audio_file |
      | spring.mp3 |
      | autumn.mp3 |

  Scenario: Override attach the audio file successful
    Given My note already has "spring.mp3"
    When I attach audio file "autumn.mp3" to my note
    Then I should see "autumn.mp3" in my note