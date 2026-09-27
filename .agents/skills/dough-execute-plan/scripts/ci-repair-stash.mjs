#!/usr/bin/env node
// Installed CLI for the CI repair pause: saves this execution checkout's
// unfinished work as one stash entry and later restores and drops only that
// entry, identified by OID, so every other writer's stash entries survive.
import { mkdtemp, readFile, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { git } from "./publication-git.mjs";

const usage =
  "usage: ci-repair-stash.mjs save --checkout PATH --label TEXT | restore --record FILE";

async function optional(cwd, ...args) {
  try {
    return (await git(cwd, ...args)).stdout.trim() || null;
  } catch {
    return null;
  }
}

function failureText(error) {
  return `${error.stdout ?? ""}\n${error.stderr ?? ""}\n${error.message ?? ""}`.trim();
}

// Staged, unstaged, and untracked paths (ignored files are never included).
async function inventory(checkout) {
  const { stdout } = await git(
    checkout,
    "status",
    "--porcelain=v1",
    "-z",
    "--untracked-files=all",
  );
  const result = { staged: [], unstaged: [], untracked: [] };
  const fields = stdout.split("\0");
  for (let index = 0; index < fields.length; index += 1) {
    const entry = fields[index];
    if (entry.length < 4) continue;
    const [x, y, path] = [entry[0], entry[1], entry.slice(3)];
    if (x === "R" || x === "C") index += 1; // skip the rename source field
    if (x === "?") {
      result.untracked.push(path);
      continue;
    }
    if (x !== " ") result.staged.push(path);
    if (y !== " ") result.unstaged.push(path);
  }
  return result;
}

function isDirty(paths) {
  return (
    paths.staged.length + paths.unstaged.length + paths.untracked.length > 0
  );
}

async function stashEntries(checkout) {
  const listed = await optional(
    checkout,
    "stash",
    "list",
    "--format=%gd%x00%H%x00%gs",
  );
  return (listed ?? "")
    .split("\n")
    .filter(Boolean)
    .map((line) => {
      const [selector, oid, subject] = line.split("\0");
      return { selector, oid, subject };
    });
}

async function writeRecord(record) {
  const directory = await mkdtemp(join(tmpdir(), "dough-ci-repair-stash-"));
  const file = join(directory, "record.json");
  await writeFile(file, `${JSON.stringify(record, null, 2)}\n`, {
    mode: 0o600,
  });
  return file;
}

export async function saveRepairStash({ checkout, label }) {
  if (!checkout || !label) throw new Error(usage);
  const root = (
    await git(resolve(checkout), "rev-parse", "--show-toplevel")
  ).stdout.trim();
  const before = await stashEntries(root);
  const record = {
    checkout: root,
    label,
    branch: await optional(root, "symbolic-ref", "--short", "-q", "HEAD"),
    head: (await git(root, "rev-parse", "HEAD")).stdout.trim(),
    ...(await inventory(root)),
    previousTop: before[0]?.oid ?? null,
    oid: null,
  };
  if (!isDirty(record)) {
    record.status = "clean";
  } else {
    let pushed = true;
    try {
      await git(root, "stash", "push", "--include-untracked", "-m", label);
    } catch (error) {
      pushed = false;
      record.status = "failed";
      record.error = failureText(error);
    }
    if (pushed) {
      const known = new Set(before.map((entry) => entry.oid));
      const created = (await stashEntries(root)).filter(
        (entry) =>
          !known.has(entry.oid) && entry.subject.endsWith(`: ${label}`),
      );
      const remaining = await inventory(root);
      if (isDirty(remaining)) record.remaining = remaining;
      if (created.length === 1) {
        record.oid = created[0].oid;
        record.status = isDirty(remaining) ? "unclean" : "stashed";
      } else {
        // The push succeeded but did not yield exactly one entry of ours.
        record.status = "ambiguous";
        record.candidates = created.map((entry) => entry.oid);
      }
    }
  }
  const file = await writeRecord(record);
  return {
    ok: record.status === "stashed" || record.status === "clean",
    record: file,
    ...record,
  };
}

async function conflictPaths(checkout, output) {
  const unmerged = (
    (await optional(checkout, "diff", "--name-only", "--diff-filter=U")) ?? ""
  )
    .split("\n")
    .filter(Boolean);
  const overwritten = [
    ...output.matchAll(/would be overwritten[^\n]*\n((?:\t[^\n]*\n?)+)/g),
  ].flatMap((match) => match[1].split("\n"));
  const reported = [
    ...output.matchAll(/Merge conflict in (.+)$/gm),
    ...output.matchAll(/^(.+) already exists, no checkout$/gm),
  ]
    .map((match) => match[1])
    .concat(overwritten)
    .map((path) => path.trim())
    .filter(Boolean);
  return [...new Set([...unmerged, ...reported])];
}

// Paths the record saved that the restored tree does not show in the same
// staged, unstaged, or untracked state.
function unrestored(record, current) {
  return ["staged", "unstaged", "untracked"].flatMap((kind) =>
    record[kind].filter((path) => !current[kind].includes(path)),
  );
}

async function dropExact(checkout, oid) {
  const entry = (await stashEntries(checkout)).find((item) => item.oid === oid);
  if (!entry) return null;
  await git(checkout, "stash", "drop", "-q", entry.selector);
  return entry.selector;
}

export async function restoreRepairStash({ record: file }) {
  if (!file) throw new Error(usage);
  const record = JSON.parse(await readFile(file, "utf8"));
  const receipt = {
    record: file,
    oid: record.oid,
    applied: false,
    dropped: null,
  };
  if (record.status === "ambiguous")
    return { ok: false, status: "ambiguous", ...receipt, paths: [] };
  if (!record.oid) return { ok: true, status: "resumed", ...receipt };
  const checkout = record.checkout;
  const entry = (await stashEntries(checkout)).find(
    (item) => item.oid === record.oid,
  );
  if (!entry) return { ok: false, status: "missing", ...receipt, paths: [] };
  try {
    await git(checkout, "stash", "apply", "--index", record.oid);
  } catch (error) {
    const output = failureText(error);
    return {
      ok: false,
      status: "conflict",
      ...receipt,
      paths: await conflictPaths(checkout, output),
      error: output,
    };
  }
  const missingPaths = unrestored(record, await inventory(checkout));
  if (missingPaths.length > 0)
    return {
      ok: false,
      status: "conflict",
      ...receipt,
      applied: true,
      paths: missingPaths,
      error: "applied work does not match the saved inventory; entry kept",
    };
  return {
    ok: true,
    status: "resumed",
    ...receipt,
    applied: true,
    dropped: await dropExact(checkout, record.oid),
  };
}

function argumentsOf(argv) {
  const result = { command: argv[0] };
  for (let index = 1; index < argv.length; index += 2) {
    if (!argv[index].startsWith("--") || index + 1 >= argv.length)
      throw new Error(`invalid argument ${argv[index]}\n${usage}`);
    result[argv[index].slice(2)] = argv[index + 1];
  }
  return result;
}

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  try {
    const { command, ...options } = argumentsOf(process.argv.slice(2));
    const operation = { save: saveRepairStash, restore: restoreRepairStash }[
      command
    ];
    if (!operation) throw new Error(usage);
    const result = await operation(options);
    process.stdout.write(`${JSON.stringify(result)}\n`);
    if (!result.ok) process.exitCode = 1;
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 2;
  }
}
