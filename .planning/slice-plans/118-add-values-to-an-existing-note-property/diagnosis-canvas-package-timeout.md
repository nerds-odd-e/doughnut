# Hosted Ubuntu canvas package timeout

## Round 1

GitHub Actions run 45303165110 attempt 1 on
`a16d83eebd95ab4a56fc78d83cdef2b0eacf7af0` reported job 138140443469
(Frontend Unit Tests 2/2) failed in the shared canvas setup, before installing
locked pnpm dependencies or running tests. APT obtained other packages from
`us-east-2.ec2.archive.ubuntu.com` and marked `libharfbuzz-subset0` as ignored.
The canvas phase reached its 600-second bound with exit 124. The initial
inventory is incomplete while the packaging job remains in progress. See
`ci-45303165110-diagnostic.json` under the checkout's Git evidence directory.

The story did not edit the shared package setup. Commit `cd689694b9` settled
a different failure in a Microsoft repository during index refresh. The
current issue is during acquisition from the Ubuntu archive. Inspect the
complete setup log and compare its network symptoms and job conditions with
successful sibling setups. Identify whether the repository unnecessarily
forces the affected route or whether the archive had an external incident.
Stop after one attempt inventory and one comparison with a sibling. If they
identify a repository defect, select one distinguishing Bash boundary case
before making a repair. Do not increase the phase deadline to mask the cause.

The current HEAD is `ce09115bcf82bb818d1bc8594f4f67636870998f`. No unfinished
product edits or running writers were present. The CI stash operation recorded
a clean tree without creating a stash entry.

### Result

The terminal attempt reported 12 successful jobs and one failed job. Its
only failed step was the shared Node setup in frontend shard 2. The full
job log identifies canvas APT installation as the failing substep. The
diagnostic parser did not classify a test failure because locked dependency
installation and the test steps had yet to start.

Both frontend shards ran concurrently on Ubuntu 26.04 image
`20261116.88.1`. They requested the same 11.5 MB of packages from the
runner's `us-east-2.ec2.archive.ubuntu.com` source. Both refreshed their
indexes in under 108 seconds. Shard 1 fetched and installed
`libharfbuzz-subset0` version `12.3.2-2`, completed canvas setup in 394
seconds, and passed its tests. In shard 2, package downloads took longer.
APT logged `Ign:57` for the same package at 01:20:02 UTC and downloaded
later packages, with the last successful download logged at 01:21:32 UTC.
There was no successful fetch of package 57 when the phase timed out at
01:21:57 UTC while acquisition was still underway.

The script uses the runner's Ubuntu source configuration and reports the
existing 600-second bound with exit 124. The story changed neither the
setup script, action, workflow, nor dependency lock. The comparison
attributes the failed attempt to package acquisition on that runner. It
does not locate the stall within the network path. No repository repair or
new test was warranted by the observed failure.

Evidence in the checkout's Git evidence directory is
`ci-45303165110-terminal-diagnostic.json`,
`ci-45303165110-job-138140443469.log` (failed), and
`ci-45303165110-job-138140443445.log` (successful).
