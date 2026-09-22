import { execFile, execFileSync } from "node:child_process";
import { setTimeout as pause } from "node:timers/promises";
import { fileURLToPath } from "node:url";

export const mailboxWorkerPath = fileURLToPath(
  new URL("./ci-mailbox.mjs", import.meta.url),
);

function workerIsRunning(pid) {
  try {
    process.kill(pid, 0);
    return true;
  } catch (error) {
    if (error.code === "ESRCH") return false;
    throw error;
  }
}

async function waitForWorkerExit(pid, timeoutMs = 1_000) {
  const deadline = Date.now() + timeoutMs;
  while (workerIsRunning(pid) && Date.now() < deadline) await pause(20);
  return !workerIsRunning(pid);
}

function expectedWorkerCommand(directory) {
  return `${process.execPath} ${mailboxWorkerPath} worker ${directory}`;
}

async function readProcessCommand(pid) {
  return new Promise((resolveCommand, rejectCommand) => {
    execFile(
      "ps",
      ["-ww", "-p", String(pid), "-o", "command="],
      (error, stdout) => {
        if (!error) {
          resolveCommand(stdout.trim());
          return;
        }
        if (error.code === 1) {
          resolveCommand(undefined);
          return;
        }
        rejectCommand(error);
      },
    );
  });
}

function readProcessCommandSync(pid) {
  try {
    return execFileSync("ps", ["-ww", "-p", String(pid), "-o", "command="], {
      encoding: "utf8",
    }).trim();
  } catch (error) {
    if (error.status === 1) return undefined;
    throw error;
  }
}

async function verifyMailboxWorker(pid, directory) {
  const command = await readProcessCommand(pid);
  if (command === undefined) return false;
  if (command !== expectedWorkerCommand(directory))
    throw new Error(`CI observer worker ${pid} does not match this mailbox`);
  return true;
}

// Read-only liveness check reusing the same identity rule as termination,
// without ever signaling a process. A PID that exists but runs a different
// command is reused/mismatched identity: this reports "unknown" rather than
// "alive" or "dead" so callers neither reassure a coordinator nor act on an
// unrelated process.
export function checkMailboxWorkerLiveness({ pid } = {}, directory) {
  if (!(Number.isSafeInteger(pid) && pid > 0)) return "unknown";
  if (!workerIsRunning(pid)) return "dead";
  const command = readProcessCommandSync(pid);
  if (command === undefined) return "dead";
  return command === expectedWorkerCommand(directory) ? "alive" : "unknown";
}

export async function terminateMailboxWorker({ pid }, directory) {
  if (!(Number.isSafeInteger(pid) && pid > 0))
    throw new Error("CI mailbox contains an invalid worker identity");
  if (!workerIsRunning(pid)) return;
  if (!(await verifyMailboxWorker(pid, directory))) return;
  process.kill(pid, "SIGTERM");
  if (await waitForWorkerExit(pid)) return;
  if (!(await verifyMailboxWorker(pid, directory))) return;
  process.kill(pid, "SIGKILL");
  if (!(await waitForWorkerExit(pid)))
    throw new Error(`CI observer worker ${pid} did not terminate`);
}
