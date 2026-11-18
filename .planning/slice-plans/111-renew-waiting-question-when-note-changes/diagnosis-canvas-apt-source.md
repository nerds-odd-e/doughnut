# Canvas dependency setup on hosted Ubuntu

## Round 1

GitHub Actions run 44340973938 attempt 1 on
`9ea958ddbf5ef712d9663b0e2a97a577cc118813` failed in jobs
134930513684 (Other Unit Tests) and 134930514906 (End-to-End tests with Database 3).
Both stopped in Linux canvas dependency setup before tests. The Ubuntu archive
indexes downloaded, the runner's Microsoft repository returned HTTP 429,
and APT exited 100. The bounded phase ended after 6 and 10 seconds within
its 600-second budget. The diagnostic output is retained in the Git evidence
directory as `ci-44340973938-1-followup.json`. Later jobs in the same attempt
still need classification as their results arrive.

The story's accepted increment touched Java recall generation and tests. The
shared canvas setup predates it. The observed failure is in an Ubuntu index
refresh that includes a repository unrelated to the named canvas packages.
Test that dependency first with one local Bash-boundary regression run using
APT substitutes. Preserve strict refresh errors for the required Ubuntu
archive sources and the existing deadline, retry, and diagnostic behavior.
Run the maintained canvas and MySQL boundary cases after repair. The next
owned publication is the first hosted observation of the changed setup.

Slice 2 is paused, with four unfinished Java paths preserved by the CI repair
stash. No local verification was active when the repair began.

The focused boundary reproduced the package-feed 429 before the production
change. APT exited 100, the canvas phase reported the failure, and the new
test failed. The first diagnosis used 2.1 active minutes.

The repair reused the Ubuntu source definitions already maintained by the
hosted image. Both canvas APT calls select that file. Refresh retains the
runner's other cached indexes. The canvas scenario suite passed with the
unavailable vendor feed, a separate required-archive failure still returned
100, and the established deadline and acquisition-retry cases passed. The
MySQL boundary suite also passed. The repair used 3.3 active minutes.
Hosted confirmation remains owned by the branch observer. Local evidence
is in the Git evidence directory under `canvas-apt-source-`.

[Runner source configuration](https://github.com/actions/runner-images/blob/d9c48363ed37529f6ef929845cda83bbdae0ef74/images/ubuntu/scripts/build/configure-apt-sources.sh)
[Ubuntu APT manual](https://manpages.ubuntu.com/manpages/noble/man8/apt-get.8.html)

The terminal diagnostic for the original attempt lists exactly the same two
failed jobs. Both stopped at canvas setup from the same Microsoft feed 429.
The independent refactor tightened the install-source observation and left
production behavior intact. The refreshed canvas and MySQL cases both passed.
