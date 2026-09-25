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

  @bundleCliE2eInstall @withCliConfig
  Scenario: CLI publishes two LFS versions and a fresh clone receives current bytes after a web save
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And the notebook "LFS Transfer Notebook" has an accepted LFS tip "payload.bin" with payload "seed-lfs-bytes" and obsolete payload "obsolete-lfs-bytes"
    When I clone the notebook "LFS Transfer Notebook" into a temporary destination using the installed CLI
    And I commit the LFS attachment "payload.bin" filled with 1024 bytes of "0x11" as "lfsVersionA"
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I commit the LFS attachment "payload.bin" filled with 2048 bytes of "0x22" as "lfsVersionB"
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And the LFS commit "lfsVersionA" remains an ancestor of "lfsVersionB"
    And the notebook "LFS Transfer Notebook" MySQL attachment "payload.bin" holds the LFS pointer for "lfsVersionB"
    When I create a title-only root note titled "Overview" in the notebook "LFS Transfer Notebook"
    And I update note "Overview" content to become "Reviewed on the web after LFS publish"
    And I clone the notebook "LFS Transfer Notebook" into a fresh temporary destination using the installed CLI
    Then the fresh clone file "payload.bin" is filled with 2048 bytes of "0x22"
    And the fresh clone LFS object cache holds only the tip digest

  @bundleCliE2eInstall @withCliConfig
  Scenario: Corrective oversized intermediate LFS commit is omitted while the tip publishes
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And the notebook "LFS Transfer Notebook" has an accepted LFS tip "payload.bin" with payload "seed-lfs-bytes" and obsolete payload "obsolete-lfs-bytes"
    When I clone the notebook "LFS Transfer Notebook" into a temporary destination using the installed CLI
    And I commit the LFS attachment "payload.bin" filled with 20971520 bytes of "0x74" as "lfsOversizedIntermediate"
    And I commit the LFS attachment "payload.bin" filled with 3145728 bytes of "0x75" as "lfsCorrectiveTip"
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And the LFS commit "lfsOversizedIntermediate" remains an ancestor of "lfsCorrectiveTip"
    And the notebook "LFS Transfer Notebook" content store lacks object for "lfsOversizedIntermediate" under attachment "payload.bin"
    And the notebook "LFS Transfer Notebook" content store has object for "lfsCorrectiveTip" under attachment "payload.bin"
    And the notebook "LFS Transfer Notebook" MySQL attachment "payload.bin" holds the LFS pointer for "lfsCorrectiveTip"

  @bundleCliE2eInstall @withCliConfig
  Scenario: Three incompressible LFS versions keep pointer history and current-only clone download
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And the notebook "LFS Transfer Notebook" has an accepted LFS tip "payload.bin" with payload "seed-lfs-bytes" and obsolete payload "obsolete-lfs-bytes"
    When I clone the notebook "LFS Transfer Notebook" into a temporary destination using the installed CLI
    And I commit the incompressible LFS attachment "payload.bin" of 3145728 bytes as "lfsRandV1"
    And I commit the incompressible LFS attachment "payload.bin" of 3145728 bytes as "lfsRandV2"
    And I commit the incompressible LFS attachment "payload.bin" of 3145728 bytes as "lfsRandV3"
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And the LFS commit "lfsRandV1" remains an ancestor of "lfsRandV3"
    And the notebook "LFS Transfer Notebook" MySQL attachment "payload.bin" holds the LFS pointer for "lfsRandV3"
    When I clone the notebook "LFS Transfer Notebook" into a fresh temporary destination using the installed CLI
    Then the fresh clone LFS object cache holds only the tip digest
    And bundle traffic stays pointer-scale versus object traffic for versions "lfsRandV1, lfsRandV2, lfsRandV3"

  @bundleCliE2eInstall @withCliConfig
  Scenario: Explicit Git LFS fetch recovers a published version after replacement and after current-row deletion
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And the notebook "LFS Transfer Notebook" has an accepted LFS tip "payload.bin" with payload "seed-lfs-bytes" and obsolete payload "obsolete-lfs-bytes"
    When I clone the notebook "LFS Transfer Notebook" into a temporary destination using the installed CLI
    And I commit the LFS attachment "payload.bin" filled with 1024 bytes of "0x11" as "lfsVersionA"
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    When I commit the LFS attachment "payload.bin" filled with 2048 bytes of "0x22" as "lfsVersionB"
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And the LFS commit "lfsVersionA" remains an ancestor of "lfsVersionB"
    And the cloned checkout names no Git remote
    When I clear the cloned checkout LFS object cache
    And I fetch LFS objects for commit "lfsVersionA" with the standard Git LFS client
    Then the cloned checkout LFS object cache holds the digest for "lfsVersionA"
    And the cached LFS object for "lfsVersionA" is filled with 1024 bytes of "0x11"
    When I remove the LFS attachment "payload.bin" from the cloned checkout
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And the notebook "LFS Transfer Notebook" has no root attachment named "payload.bin"
    And the notebook "LFS Transfer Notebook" content store has object for "lfsVersionA" under attachment "payload.bin"
    When I clear the cloned checkout LFS object cache
    And I fetch LFS objects for commit "lfsVersionA" with the standard Git LFS client
    Then the cached LFS object for "lfsVersionA" is filled with 1024 bytes of "0x11"

  @bundleCliE2eInstall @withCliConfig
  Scenario: Same-checkout pull keeps a local note and image over a web save, then publishes
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    When I clone the notebook "LFS Transfer Notebook" into a temporary destination using the installed CLI
    And I add and commit the following note at "Shopping list.md" in the cloned checkout:
      """
      ---
      type: Note
      ---
      Milk
      """
    And I commit the LFS attachment "photo.png" filled with 1024 bytes of "0x11" and the following edit to "Shopping list.md" as "lfsLocalImage":
      """
      ---
      type: Note
      ---
      Milk and eggs
      """
    And I create a title-only root note titled "Overview" in the notebook "LFS Transfer Notebook"
    And I update note "Overview" content to become "Reviewed on the web"
    And I pull the cloned checkout using the installed CLI
    Then the cloned checkout file "Overview.md" is:
      """
      ---
      type: Note
      ---
      Reviewed on the web
      """
    And the cloned checkout file "photo.png" is filled with 1024 bytes of "0x11"
    And the cloned checkout LFS object cache holds only the tip digest for "photo.png"
    When I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the rebased local head as the accepted head
    And the note "LFS Transfer Notebook/Shopping list" in Donut should have content "Milk and eggs"

  @bundleCliE2eInstall @withCliConfig
  Scenario: A picture uploaded on the web arrives in the owner's clone beside its note
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And I have a notebook "LFS Transfer Notebook" with notes:
      | Title | Folder  | Content   |
      | force | physics | Body text |
    When I upload an image from fixture "moon.jpg" to the note "force"
    And I clone the notebook "LFS Transfer Notebook" into a temporary destination using the installed CLI
    Then the cloned checkout file "physics/moon.jpg" has the bytes of fixture "moon.jpg"
    And the cloned checkout file "physics/force.md" is:
      """
      ---
      type: Note
      image: moon.jpg
      ---
      Body text
      """

  @bundleCliE2eInstall @withCliConfig
  Scenario: Explicit Git LFS fetch of an omitted oversized intermediate reports unavailable
    Given the backend is serving the CLI and install script
    And the CLI is installed from localhost
    And the notebook "LFS Transfer Notebook" has an accepted LFS tip "payload.bin" with payload "seed-lfs-bytes" and obsolete payload "obsolete-lfs-bytes"
    When I clone the notebook "LFS Transfer Notebook" into a temporary destination using the installed CLI
    And I commit the LFS attachment "payload.bin" filled with 20971520 bytes of "0x74" as "lfsOversizedIntermediate"
    And I commit the LFS attachment "payload.bin" filled with 3145728 bytes of "0x75" as "lfsCorrectiveTip"
    And I publish the cloned checkout using the installed CLI
    Then the installed CLI reports the committed change as the accepted head
    And the notebook "LFS Transfer Notebook" content store lacks object for "lfsOversizedIntermediate" under attachment "payload.bin"
    When I clear the cloned checkout LFS object cache
    And I attempt to fetch LFS objects for commit "lfsOversizedIntermediate" with the standard Git LFS client
    Then the standard Git LFS historical fetch reports the object unavailable
    And the notebook "LFS Transfer Notebook" content store lacks object for "lfsOversizedIntermediate" under attachment "payload.bin"
