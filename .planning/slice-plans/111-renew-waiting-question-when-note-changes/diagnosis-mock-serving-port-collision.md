# Private OpenAI mock serving port in CI

## Round 1

GitHub Actions run 44351190128 attempt 1 failed on the accepted
`e669f8a5f3b610f5c77652d9deaf92e2c12b7888` revision. Job 134966957587
(Linting & Types Gen for Frontend) reached the browser-worktree isolation
suite. The real private OpenAI mock test refused to start because it found
a foreign serving listener on port 37357 (PID and process group 8374).
The explicit ownership check refused before mutating the mock. The remaining
lint steps did not run, which left downstream proof reports absent. The CI
job log and diagnostic are retained under `ci-44351190128-1-` in the
checkout's Git evidence directory.

The changed story code is Java recall. The suspect interval exists in the
earlier shared mock implementation. Its allocator closes all reserved sockets
before the child starts and the serving listener is requested after management
startup. Check whether a concurrent local mock can acquire the same socket
during that interval. Reproduce deterministically with the existing mock and
ownership test helpers before repairing it. Preserve fixed-port refusal,
owned cleanup and foreign listener survival. Prove the browser isolation
suite after the focused repair. Limit the first round to one controlled
reproduction and inspect its exact signature.

Slice 4 is paused. Eight tracked Java changes and one untracked controller
test were saved with the documented CI repair stash. Its focused backend
command had already passed 204 cases. Remaining jobs from the GitHub attempt
will be classified when results arrive.

The controlled reproduction bound the real allocator's reported serving port
before management startup. The real ownership check returned the same foreign
serving listener refusal. Round 1 took 1.8 active minutes. The terminal hosted
diagnosis identifies that test as the attempt's sole failure.

The repair holds each serving reservation until its bind handoff and releases
the remaining sockets on service startup failure. It keeps the explicit
foreign-listener check and owned cleanup. The approved size reorganization
moved the existing imposter HTTP client and child lifecycle into cohesive
modules while retaining the startup module's public exports. Its 347 lines
became 224. The repair took 2.9 active minutes. The focused and full browser
worktree isolation tests passed, including all 144 cases in the full command.
Evidence is in the Git evidence directory under `mock-port-`.

The independent refactor review kept the repair and its terminal proof.
It traced release through synchronous spawn error, fixed-port refusal,
partial multi-service startup and normal cleanup. The existing foreign
ownership guard remains in the final socket-to-bind handoff.
