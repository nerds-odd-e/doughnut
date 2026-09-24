#!/usr/bin/env node
// Installed CLI for authorized queued startup.
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { startQueuedExecution } from "./execution-start-operation.mjs";

export { startQueuedExecution } from "./execution-start-operation.mjs";

function argumentsOf(argv) {
  if (argv[0] !== "start")
    throw new Error(
      "usage: execution-start.mjs start --integration PATH --workspace PATH --branch NAME --identity ID --publisher-id ID --mode trunk|story-branch --remote NAME --target BRANCH --push-authorized --workspace-authorized [--plan PATH] [--host claude|codex|cursor] [--model TEXT] [--declared-owner ID --requester ID] [--starting-revision SHA --candidate-sha SHA]",
    );
  const result = {};
  for (let index = 1; index < argv.length; index += 1) {
    const flag = argv[index];
    if (flag === "--push-authorized" || flag === "--workspace-authorized") {
      result[
        flag === "--push-authorized" ? "pushAuthorized" : "workspaceAuthorized"
      ] = true;
      continue;
    }
    if (!flag.startsWith("--") || index + 1 >= argv.length)
      throw new Error(`invalid argument ${flag}`);
    const key = flag
      .slice(2)
      .replace(/-[a-z]/g, (match) => match[1].toUpperCase());
    result[key] = argv[++index];
  }
  return result;
}

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  try {
    const result = await startQueuedExecution(
      argumentsOf(process.argv.slice(2)),
    );
    process.stdout.write(`${JSON.stringify(result)}\n`);
    if (!result.ok) process.exitCode = 1;
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 2;
  }
}
