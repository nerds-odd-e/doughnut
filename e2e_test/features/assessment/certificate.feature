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
      | China         | Who was the first emperor of China?   | Qin-Shi         | eiei                   | true     |

    And I set the number of questions per assessment of the notebook "Countries" to 5

  Scenario: Show a certificate when passed the assessment.
    When I do the assessment on "Countries" in the bazaar with the following answers:
      | Question                                | Answer            |
      | Where in the world is Singapore?        | Asia              |
      | Most famous food of Vietnam?            | Pho               |
      | What is the capital city of Japan?      | Tokyo             |
      | What is the capital city of Thailand?   | Bangkok           |
      | Who was the first emperor of China?     | Qin-Shi           |
    
    Then I should see view a certificate button 