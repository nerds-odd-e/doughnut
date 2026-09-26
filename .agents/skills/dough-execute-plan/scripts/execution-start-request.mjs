// Validates and normalizes a queued-start or admission request before any
// Git work.
import { resolve } from "node:path";
import {
  agentModes,
  agentReportError,
} from "../../dough-product-backlog/scripts/product-backlog-agent-profile.mjs";
import { stopped } from "./workspace-publication-ownership.mjs";

// The normalized request, or the stop that refuses it.
export function startRequest(requestInput) {
  const required = [
    "integration",
    "workspace",
    "branch",
    "identity",
    "publisherId",
    "mode",
    "target",
  ];
  for (const field of required)
    if (!requestInput[field])
      return stopped("invalid-request", { error: `missing ${field}` });
  const request = {
    ...requestInput,
    integration: resolve(requestInput.integration),
    workspace: resolve(requestInput.workspace),
  };
  if (request.startingRevision || request.candidateSha) {
    if (!request.startingRevision || !request.candidateSha)
      return stopped("invalid-request", {
        error: "resume requires starting revision and candidate SHA",
      });
    request.retained = {
      workspace: request.workspace,
      branch: request.branch,
      startingRevision: request.startingRevision,
      candidateSha: request.candidateSha,
    };
  }
  if (request.admit === true)
    for (const field of ["link", "title"])
      if (!request[field])
        return stopped("invalid-request", {
          error: `admission requires ${field}`,
        });
  const reportError = agentReportError(request);
  if (reportError) return stopped("invalid-request", { error: reportError });
  if (
    !agentModes.includes(request.mode) ||
    request.pushAuthorized !== true ||
    request.workspaceAuthorized !== true
  ) {
    return stopped("authority-required", {
      error:
        "mode, workspace and trunk publication authority must be established",
    });
  }
  if (request.integration === request.workspace)
    return stopped("invalid-request", {
      error: "queued work requires a separate owned workspace",
    });
  return { ok: true, request };
}
