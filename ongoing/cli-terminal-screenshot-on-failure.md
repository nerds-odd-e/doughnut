# Plan: CLI Terminal Screenshot on E2E Failure

## Context

When CLI-related E2E tests fail, there is no visual capture of the terminal state. Cypress captures browser screenshots on failure and uploads `e2e_test/screenshots/` as the `cypress-screenshots` artifact. We want CLI terminal output to be captured similarly and included in the same artifact.

**Approach (Option 1)**: Write CLI capture files directly into `e2e_test/screenshots/`. The existing CI workflow already uploads that folder on failure—no workflow change needed.

## Phased Plan

### Phase 1: Capture CLI output when tasks succeed

**User value**: When an assertion fails after a CLI task returns (e.g. "I should see X in the last command output"), the terminal output is in the screenshots artifact.

- Add a helper `writeCliOutputCapture(output: string, screenshotsDir: string)` in `e2e_test/config/common.ts` that:
  - Ensures `screenshotsDir` exists (`mkdirSync(..., { recursive: true })`)
  - Writes to `{screenshotsDir}/cli_output.txt`
- Invoke it from all CLI tasks that return stdout **before** resolving:
  - `runCliDirectWithInput`
  - `runCliDirectWithInputAndPty`
  - `runCliDirectWithArgs`
  - `runInstalledCli` (both PTY and spawn branches)
  - `runCliDirectWithGmailAdd` (write `stdout`)
  - `runCliDirectWithLastEmail`
- Use `config.screenshotsFolder` passed into `setupNodeEvents` (available on `config`) to get the path.
- **Verification**: Run a CLI scenario, induce an assertion failure (e.g. expect wrong text), confirm `e2e_test/screenshots/cli_output.txt` exists with the terminal output. Run full E2E with one failure and confirm the artifact includes the file.

**Deliverable**: CLI output captured on success; available in artifact when subsequent assertions fail.

---

### Phase 2: Capture CLI output when tasks reject

**User value**: When the CLI times out, exits non-zero, or crashes, the partial terminal output is still captured for debugging.

- In `runCliInPty` (cliPtyRunner.ts): before calling `reject(...)` in both the timeout handler and `onExit` (exitCode !== 0), write `stdout` to the capture file. The runner does not have `config`—pass `screenshotsDir` as an optional parameter to `runCliInPty`, or export a `setCliCaptureDir(dir)` that tasks call once at startup.
- In `common.ts` tasks that use `spawn` and reject on non-zero exit:
  - `runCliDirectWithArgs`: write `stdout` before rejecting
  - `runInstalledCli` (spawn branch): write `stdout` before rejecting
  - `runCliDirectWithGmailAdd`: already resolves with `{ stdout, exitCode }`; on failure path, write stdout
  - `runCliDirectWithLastEmail`: write `stdout` before rejecting
- Ensure `runCliInPty` can access the screenshots path: add optional `opts.screenshotsDir` and pass it from the calling tasks. If not provided, skip writing (backward compatibility for non-E2E usage).
- **Verification**: Temporarily force a timeout or non-zero exit in a CLI scenario, confirm `cli_output.txt` contains the partial output. Restore and run normally.

**Deliverable**: Terminal output captured even when the CLI process fails; artifact includes output for timeout/crash failures.

---

### Phase 3 (optional): Spec-specific filenames

**User value**: When multiple CLI specs fail in different jobs (matrix), or when reviewing artifacts, each spec’s capture is clearly named.

- Pass `Cypress.spec?.relative` from step definitions to tasks. This requires adding an optional `specName?: string` to task params and threading it through.
- Write to `cli_{basename(specName, '.feature')}.txt` instead of `cli_output.txt`.
- **Verification**: Run CLI specs, induce failures, confirm filenames like `cli_cli_recall.txt` in the artifact.

**Deliverable**: Clearer artifact organization; can be deferred if Phase 1–2 suffice.
