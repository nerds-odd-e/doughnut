Feature: Get Certificate by an assessment.
  As a learner, I want to get the certificate when I do assessment passed,
  so that I can show my certificate on my portfolio.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Countries" and notes:
      | Topic        | Parent Topic |
      | Singapore    | Countries    |
      | Vietnam      | Countries    |
      | Japan        | Countries    |
      | Thailand     | Countries    |
      | China        | Countries    |
      

    And notebook "Countries" is shared to the Bazaar
    And there are questions for the note:
      | Note Topic    | Question                              | Answer          | One Wrong Choice       | Approved |
      | Singapore     | Where in the world is Singapore?      | Asia            | europe                 | true     |
      | Vietnam       | Most famous food of Vietnam?          | Pho             | bread                  | true     |
      | Japan         | What is the capital city of Japan?    | Tokyo           | kyoto                  | true     |
      | Thailand      | What is the capital city of Thailand? | Bangkok         | DAS                    | true     |
      | China         | Who was the first emperor of China?   | Qin Shi         | eiei                   | true     |

    And I set the number of questions per assessment of the notebook "Countries" to 5

  @ignore
  Scenario: Show a certificate when passed the assessment.
    # When I do assessment correct more than 80% score:
    When I do the assessment on "Countries" in the bazaar with the following answers:
      | Question                                | Answer            |
      | Where in the world is Singapore?        | Asia              |
      | Most famous food of Vietnam?            | Pho               |
      | What is the capital city of Japan?      | Tokyo             |
      | What is the famous food in Thailand?    | Bangkok           |

    
    Then I should see the score "Your score: <ExpectedScore> / 5" at the end of assessment
    # Then I should see the certificate of "Countries"
    # And I should see the view certificate button

    Examples:
      | ExpectedScore |
      | 5             |

#  Scenario: Show a certificate when passed the assessment.
#     When I do assessment correct equal 80% score:
#     # When I do the assessment on "Countries" in the bazaar with the following answers:
#     #   | Question                                | Answer            |
#     #   | Where in the world is Singapore?        | <SingaporeAnswer> |
#     #   | Most famous food of Vietnam?            | <VietnamAnswer>   |
#     #   | What is the capital city of Japan?      | <JapanAnswer>     |
#     #   | What is the capital city of Thailand?   | <ThailandAnswer>     |

    
#     # Then I should see the score "Your score: <ExpectedScore> / 4" at the end of assessment
#     Then I should see the certificate of "Countries"
#     And I should see the view certificate button
#     When I view the certificate
#     Then I should see the "fgisdhgio"

#     Examples:
#       | SingaporeAnswer | VietnamAnswer | JapanAnswer | ThailandAnswer  | ExpectedScore |
#       | Asia            | Pho           | Tokyo       | Chiang Mai      | 4             |
     
#  Scenario: Hide a certificate when passed the assessment.
#     When I do assessment correct less than 80% score:
      
#     Then I should not see the certificate

