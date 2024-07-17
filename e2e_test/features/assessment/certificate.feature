Feature: Get Certificate by an assessment.
  As a learner, I want to get the certificate when I do assessment passed,
  so that I can show my certificate on my portfolio.

  Scenario: As a learner, I receive a certificate when pass the assessment.
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
    # And The notebook has 80 percent score criteria to pass the assessment

    When I get <score> percent score when do the assessment on "Countries"
    Then I should "<receive or not>" my certificate of "Countries"

    Examples:
    | score | receive or not |
    | 100   | receive        |
    | 80    | receive        |
    | 20    | not receive    |
