# Startup readiness HTTP response

The original full backend command failed one of 3,225 tests. The startup
readiness test recorded 404 while migration was held. Original XML, log,
focused probe, full instrumented probe and their run records are in the
checkout's Git evidence directory under `slice-4-healthcheck-`.

Both probes returned 503 and 200. Their instrumentation logged before the
web server published its port and before common-pool startup. Those prints
can perturb first-request timing. The next bounded experiment preserves
the entire successful path through the first HTTP response. It captures
the response body, protocol, address, mapping and readiness only after an
unexpected response. It will run the complete local backend suite once,
under the same checkout and ordinary test options. If the failure still
does not recur, this experiment leaves the cause open.

Round 1 took 0.9 active minutes. The full isolated Mac arm64 suite passed
3,223 cases with two skipped and zero failures. The run left the agreed 503
and 200 assertions intact and emitted no unexpected-response record. The
temporary test patch was reversed by its exact diff. The run record, log,
patch and readiness XML are in the Git evidence directory under
`slice-4-readiness-failure-only`.

The original 404 remains unexplained. The focused diagnostic, full
instrumented diagnostic and failure-only full run did not recover the
original response's identity or body. This round supplied no cause or
repair. Resume by capturing a failing response under a discriminating
experiment or with an explicit exception for this local readiness result.

## Round 2

The developer authorized another bounded diagnostic round. Test whether the
unexpected reply depends on the readiness client's default proxy or its HTTP/2
upgrade preference, and observe whether the request reaches the embedded
Tomcat engine and MVC handler. Use at most twelve startups in one focused JVM,
four per transport variant. Retain the real Flyway latch and readiness assertions.
The variants change one client setting at a time: default, direct connection,
and HTTP/1.1. Record the server-side dispatch signals and response in each trial.
Stop at the first failed case or after twelve trials. This tests repeated cold
connectors and distinguishes client routing from server dispatch. It leaves the
surrounding full-suite test order outside its scope.

Round 2 took 1.6 active minutes and ran all twelve trials in 34.8 seconds
of JUnit execution, four per transport. It used the Mac arm64 worktree
and its isolated MySQL database. Every
trial arrived at the intended embedded Tomcat engine and reached
`HealthCheckController#ping`. The server and client agreed on 503 during the
held migration and 200 after release. The default selector reported DIRECT.
The default and direct-client variants sent an `h2c` upgrade header and
received HTTP/1.1. The explicit HTTP/1.1 variant omitted that header. None
reproduced the 404 under these conditions. The original request's route
remains unknown. The log, XML, run record and temporary probe are retained as
`readiness-round-2-ingress*` in the checkout's Git evidence directory.

## Round 3

The prior focused diagnostic ran the readiness test before its peer fixtures.
Exercise the prerequisite ordering observed in the full suite instead. Force
the two embedded-server peers and the regular readiness controller test to run
before six readiness startups, all using the original default HTTP client.
Capture the order from JUnit timestamps and keep the Tomcat ingress probe so
an unexpected reply can be attributed. Stop after the first readiness failure
or six held migrations. This is an ordering injection and leaves the rest of
the full-suite history outside its scope.

Round 3 took 1.4 active minutes and passed all 21 cases. JUnit timestamps
confirmed that `BadRequestRecoveryTest`,
`DevelopmentAuthenticationConfigurationTest` and `HealthCheckControllerTest`
ran in that order before the six startups in `HealthCheckStartupReadinessTest`.
In every startup the client connected directly and the intended Tomcat engine
routed both requests to `HealthCheckController#ping`. The client and server
agreed on 503 during migration and 200 after release. The results, order,
ingress traces, run record and temporary patch are in the checkout's Git
evidence directory under `readiness-round-3-order*`. The patch was reversed
and the readiness test matches the published revision.

The three rounds took 3.9 active minutes. The client variants and the
selected fixture order passed throughout. Identifying the cause of the original
404 still requires capturing a failure with the listener and dispatch path.

## Round 4

The developer requested another attempt. The fixture asks Tomcat for a port
on its default bind address, then sends its requests specifically to IPv4
loopback. Test whether macOS can give a wildcard listener and an IPv4
loopback listener the same port. First check socket binding in both orders
using only experiment-owned sockets. If the platform permits this, challenge
the real fixture with one local HTTP responder and compare an explicit
loopback binding. If it prevents this, use one ordinary full-suite retry
with listener and dispatch evidence on failure. Stop after these controls.

On 2026-11-19, the local Darwin arm64 socket control allowed simultaneous
wildcard and loopback listeners in both bind orders. Two listeners on the
same loopback address failed to bind the same port. With the real test
fixture on the default wildcard address and port zero, the controlled local
responder shared port 56143 and received the first GET. The existing test
assertion failed with 404, the responder's identifying body, and 200 after
migration. Changing only the fixture's server address to its request
destination, 127.0.0.1, made the competing bind fail on port 57551 and the
original assertions pass, including 503 Starting then 200. This isolates a
reproducible fixture defect independent of the batch changes. The original
failure report cannot identify the historical listener. The focused tests
and OS control are in the Git evidence directory under `readiness-round-4-*`.
Round 4 took 1.7 active minutes, 5.6 minutes across all rounds.
