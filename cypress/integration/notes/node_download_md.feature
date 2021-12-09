Feature: Download Note to .md file
    As a user, I want to be able to download note that currently open as .md file.


  Background:
    Given I've logged in as an existing user
    When I create top level note with:
      | Title    | Description  | Picture Url      | Url | 
      | Singapore | Singapore is great | https://i.gr-assets.com/images/S/compressed.photo.goodreads.com/books/1255573980l/1713426.jpg  | http://localhost:3000/ |

  @featureToggle  @ignore
  Scenario: User download current note
    Then I download note
    Then There is a "Singapore.md" file downloaded
    And the file "Singapore.md" content is
    """md
    # Singapore
    Singapore is great
    !(image)[https://i.gr-assets.com/images/S/compressed.photo.goodreads.com/books/1255573980l/1713426.jpg]
    (http://localhost:3000/)[http://localhost:3000/]
    """