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

  @bundleCliE2eInstall @withCliConfig
  Scenario: CLI clone hydrates the current LFS tip into a clean usable checkout
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And the notebook "LFS Transfer Notebook" has an accepted LFS tip "payload.bin" with payload "current-usable-lfs-bytes" and obsolete payload "obsolete-lfs-bytes"
    When I clone the notebook "LFS Transfer Notebook" into a temporary destination using the installed CLI
    Then the cloned checkout is a clean main branch
    And the cloned checkout file "payload.bin" is exactly "current-usable-lfs-bytes"
    And the cloned checkout LFS object cache holds only the tip digest for "payload.bin"
    And the cloned checkout does not track credentials
    And the cloned checkout local Git config records the LFS endpoint for "LFS Transfer Notebook"

  @bundleCliE2eInstall @withCliConfig
  Scenario: Failed CLI clone leaves an existing destination untouched
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And the notebook "LFS Transfer Notebook" has an accepted LFS tip "payload.bin" with payload "current-usable-lfs-bytes" and obsolete payload "obsolete-lfs-bytes"
    And an existing destination already contains "sentinel.txt" with "pre-existing"
    When I clone the notebook "LFS Transfer Notebook" expecting rejection from the installed CLI into that existing destination
    Then the existing destination file "sentinel.txt" is still "pre-existing"
    And I should see "already exists" in the non-interactive output
