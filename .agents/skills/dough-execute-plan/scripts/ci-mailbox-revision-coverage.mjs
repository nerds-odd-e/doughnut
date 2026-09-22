import { existsSync, mkdirSync, readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { isFullGitRevision } from "./ci-revisions.mjs";
import { publishJson } from "./ci-mailbox-json-file.mjs";

const missingRevisionPollLimit = 3;
const revisionDirectory = (directory) => join(directory, "coverage");

function revisionPath(directory, sha) {
  if (!isFullGitRevision(sha))
    throw new Error("Expected a full Git revision SHA");
  return join(revisionDirectory(directory), `${sha.toLowerCase()}.json`);
}

export function registerPushedRevision(directory, sha) {
  const path = revisionPath(directory, sha);
  const normalized = sha.toLowerCase();
  mkdirSync(revisionDirectory(directory), { recursive: true, mode: 0o700 });
  if (!existsSync(path))
    publishJson(revisionDirectory(directory), `${normalized}.json`, {
      sha: normalized,
      state: "unchecked",
      missingPolls: 0,
    });
  return readRevisionCoverage(directory).find(
    (revision) => revision.sha === normalized,
  );
}

export function readRevisionCoverage(directory) {
  const coverage = revisionDirectory(directory);
  if (!existsSync(coverage)) return [];
  return readdirSync(coverage)
    .filter(
      (name) =>
        name.toLowerCase().endsWith(".json") &&
        isFullGitRevision(name.slice(0, -5)),
    )
    .sort()
    .map((name) => JSON.parse(readFileSync(join(coverage, name), "utf8")));
}

function preferredAttempt(attempts) {
  return (
    attempts.find(
      ({ status, conclusion }) =>
        status === "completed" && conclusion === "success",
    ) ??
    attempts.find(({ status }) => status !== "completed") ??
    attempts[0]
  );
}

function observedRevision(revision, attempt) {
  let state = "failure";
  if (attempt.status !== "completed") state = "pending";
  else if (attempt.conclusion === "success") state = "success";
  else if (attempt.conclusion === "cancelled") state = "incomplete";
  return {
    ...revision,
    state,
    missingPolls: 0,
    checkedBy: { runId: attempt.databaseId, attemptId: attempt.attempt },
  };
}

export function observeRevisionCoverage(directory, runs, request) {
  const events = [];
  for (const revision of readRevisionCoverage(directory)) {
    const matches = runs.filter(
      ({ headSha }) => headSha?.toLowerCase() === revision.sha,
    );
    let next;
    const attempt = preferredAttempt(matches);
    if (attempt) {
      next = observedRevision(revision, attempt);
    } else if (["success", "failure", "incomplete"].includes(revision.state)) {
      next = revision;
    } else {
      const missingPolls = (revision.missingPolls ?? 0) + 1;
      next = {
        ...revision,
        state:
          missingPolls >= missingRevisionPollLimit
            ? "uncovered"
            : revision.state,
        missingPolls,
      };
      if (next.state === "uncovered" && revision.state !== "uncovered")
        events.push({
          type: "CI_COVERAGE_UNAVAILABLE",
          repo: request.repo,
          branch: request.branch,
          sha: revision.sha,
          reason: `No CI attempt for pushed revision after ${missingRevisionPollLimit} discovery polls.`,
        });
    }
    publishJson(revisionDirectory(directory), `${revision.sha}.json`, next);
  }
  return events;
}
