Feature: Notebook Git LFS authenticated transfer
  As a notebook owner or reader, I want the standard Git LFS client to upload and
  download the same verified attachment bytes through Donut's authenticated
  endpoint so knowing a hash alone never grants access.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LFS Transfer Notebook"
    And I remember the notebook id of "LFS Transfer Notebook" for LFS transfer
    And I have a valid Donut Access Token with label "E2E LFS Token"

  Scenario: Standard Git LFS client uploads and downloads exact content
    When I upload and download attachment bytes "exact-lfs-payload-bytes" with the standard Git LFS client
    Then the standard Git LFS client round trip keeps the exact SHA-256 digest

  Scenario: Unauthorized Git LFS upload is refused
    Given I am re-logged in as "another_old_learner"
    And I have a valid Donut Access Token with label "Stranger LFS Token"
    When I attempt to upload attachment bytes "denied-lfs-payload" with the standard Git LFS client
    Then the standard Git LFS client is refused authorization
