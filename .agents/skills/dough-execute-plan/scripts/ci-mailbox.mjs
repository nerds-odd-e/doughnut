import { spawn } from "node:child_process";
import { once } from "node:events";
import { existsSync, watch, writeFileSync } from "node:fs";
import { join } from "node:path";
import {
  checkoutRoot,
  createMailbox,
  mailboxRoot,
  readMailbox,
  receiptPrefix,
} from "./ci-mailbox-location.mjs";
import { isDirectCliEntry } from "./ci-direct-entry.mjs";
import {
  publishMailboxEvent,
  readWorkerIdentity,
  recordLostTerminalResult,
  recordTerminalResult,
  recordWorkerIdentity,
  terminalResultDeadlineCode,
  waitForTerminalResult,
} from "./ci-mailbox-store.mjs";
import {
  observeRevisionCoverage,
  readRevisionCoverage,
  registerPushedRevision,
} from "./ci-mailbox-revision-coverage.mjs";
import {
  mailboxWorkerPath,
  terminateMailboxWorker,
} from "./ci-mailbox-worker-process.mjs";
import { executionBudgetMs, watchCiExecution } from "./watch-ci-execution.mjs";
import { awaitRevision } from "./ci-mailbox-await.mjs";

export {
  checkoutRoot,
  createMailbox,
  mailboxRoot,
  readMailbox,
  receiptPrefix,
} from "./ci-mailbox-location.mjs";
export {
  publishMailboxEvent,
  readDeliveryProgress,
  readMailboxEvents,
  readWorkerIdentity,
  recordDeliveryProgress,
  recordWorkerIdentity,
  workerLossReason,
} from "./ci-mailbox-store.mjs";
export {
  readRevisionCoverage,
  registerPushedRevision,
} from "./ci-mailbox-revision-coverage.mjs";
export { mailboxWorkerLoss } from "./ci-mailbox-worker-process.mjs";

const resultPrefix = "CI_OBSERVER_RESULT ";
export async function runMailboxWorker(
  directory,
  { observe, onRecord, root = checkoutRoot, storage = mailboxRoot } = {},
) {
  const request = readMailbox(directory, root, storage);
  const abort = new AbortController();
  const stop = () => {
    if (existsSync(join(directory, "stop"))) abort.abort();
  };
  const subscription = watch(directory, stop);
  const stopFallback = setInterval(stop, 100);
  stopFallback.unref();
  stop();
  const recordEvent = (event) => {
    const sequence = publishMailboxEvent(directory, event);
    onRecord?.({ sequence, event });
  };
  let status;
  try {
    let event;
    if (!abort.signal.aborted)
      event = await (observe ?? watchCiExecution)({
        ...request,
        signal: abort.signal,
        emit: recordEvent,
        observeCoverage: (runs, observedAt, discoverAncestorCandidates) =>
          observeRevisionCoverage(directory, runs, request, observedAt, {
            discoverAncestorCandidates,
          }),
        registeredRevisions: async () =>
          readRevisionCoverage(directory).map(({ sha }) => sha),
      });
    status = abort.signal.aborted ? "stopped" : "finished";
    if (!abort.signal.aborted && event) recordEvent(event);
  } catch (error) {
    status = abort.signal.aborted ? "stopped" : "finished";
    if (!abort.signal.aborted)
      recordEvent({
        type: "CI_MONITOR_UNAVAILABLE",
        repo: request.repo,
        sha: request.sha,
        reason: String(error).slice(0, 600),
      });
  } finally {
    subscription.close();
    clearInterval(stopFallback);
  }
  recordTerminalResult(directory, request, status);
}
export async function streamMailboxWorker(request, options = {}) {
  const directory = createMailbox(request, options);
  const stopOnSignal = () => requestMailboxStop(directory, options);
  if (options.stopOnSignal) {
    process.once("SIGINT", stopOnSignal);
    process.once("SIGTERM", stopOnSignal);
  }
  options.write?.(
    `${receiptPrefix}${JSON.stringify({ directory, pid: process.pid })}\n`,
  );
  try {
    await runMailboxWorker(directory, {
      ...options,
      onRecord: (record) => options.write?.(`${JSON.stringify(record)}\n`),
    });
  } finally {
    if (options.stopOnSignal) {
      process.removeListener("SIGINT", stopOnSignal);
      process.removeListener("SIGTERM", stopOnSignal);
    }
  }
  return directory;
}
export function requestMailboxStop(directory, options = {}) {
  readMailbox(directory, options.root, options.storage);
  writeFileSync(join(directory, "stop"), "", { mode: 0o600 });
}
async function stopMailbox(directory) {
  requestMailboxStop(directory);
  try {
    return await waitForTerminalResult(directory);
  } catch (error) {
    if (error.code !== terminalResultDeadlineCode) throw error;
    await terminateMailboxWorker(readWorkerIdentity(directory), directory);
    return recordLostTerminalResult(directory);
  }
}
async function startMailbox(request) {
  const validRepository = /^[\w.-]+\/[\w.-]+$/.test(request.repo ?? "");
  const validExecution =
    request.mode === "execution" &&
    validRepository &&
    typeof request.branch === "string" &&
    request.branch.length > 0 &&
    Number.isFinite(request.maxDurationMs) &&
    request.maxDurationMs > 0;
  if (!validExecution)
    throw new Error("Expected --execution OWNER/REPO BRANCH [BUDGET_MS]");
  const directory = createMailbox(request);
  const child = spawn(
    process.execPath,
    [mailboxWorkerPath, "worker", directory],
    {
      cwd: checkoutRoot,
      detached: true,
      stdio: "ignore",
    },
  );
  await once(child, "spawn");
  recordWorkerIdentity(directory, { pid: child.pid });
  child.unref();
  return directory;
}

export function probeMailbox(options = {}) {
  const directory = createMailbox({ probe: true }, options);
  publishMailboxEvent(directory, { type: "CI_MONITOR_READY" });
  recordTerminalResult(directory, { probe: true }, "finished");
  return directory;
}

if (isDirectCliEntry(import.meta.url, process.argv[1])) {
  const [command, ...args] = process.argv.slice(2);
  if (command === "worker") {
    await runMailboxWorker(args[0]);
  } else if (["start", "stream"].includes(command)) {
    const [, repo, branch, budget] = args;
    const request = {
      mode: args[0] === "--execution" ? "execution" : undefined,
      repo,
      branch,
      maxDurationMs: budget ? Number(budget) : executionBudgetMs,
    };
    if (command === "stream") {
      const directory = await streamMailboxWorker(request, {
        write: (output) => process.stdout.write(output),
        stopOnSignal: true,
      });
      const terminal = await waitForTerminalResult(directory);
      process.stdout.write(
        `${resultPrefix}${JSON.stringify({ directory, terminal })}\n`,
      );
    } else {
      const directory = await startMailbox(request);
      process.stdout.write(
        `${receiptPrefix}${JSON.stringify({ directory })}\n`,
      );
    }
  } else if (command === "probe") {
    process.stdout.write(
      `${receiptPrefix}${JSON.stringify({ directory: probeMailbox() })}\n`,
    );
  } else if (command === "register-push") {
    const [directory, sha] = args;
    readMailbox(directory);
    const revision = registerPushedRevision(directory, sha);
    process.stdout.write(
      `${receiptPrefix}${JSON.stringify({ directory, revision })}\n`,
    );
  } else if (command === "await-revision") {
    const [directory, sha] = args;
    const cancellation = new AbortController();
    const cancel = () => cancellation.abort();
    process.once("SIGINT", cancel);
    process.once("SIGTERM", cancel);
    try {
      const awaited = await awaitRevision(directory, sha, {
        cancellation: cancellation.signal,
      });
      process.stdout.write(`${receiptPrefix}${JSON.stringify(awaited)}\n`);
    } finally {
      process.removeListener("SIGINT", cancel);
      process.removeListener("SIGTERM", cancel);
    }
  } else if (command === "stop") {
    const directory = args[0];
    const terminal = await stopMailbox(directory);
    process.stdout.write(
      `${receiptPrefix}${JSON.stringify({ directory, terminal })}\n`,
    );
  } else {
    throw new Error(
      "Usage: ci-mailbox.mjs probe | start --execution OWNER/REPO BRANCH [BUDGET_MS] | stream --execution OWNER/REPO BRANCH [BUDGET_MS] | register-push DIRECTORY SHA | await-revision DIRECTORY SHA | stop DIRECTORY",
    );
  }
}
