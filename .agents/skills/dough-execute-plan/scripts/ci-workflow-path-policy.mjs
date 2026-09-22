// Narrow, dependency-free reader for a GitHub Actions workflow's
// `paths-ignore` triggers. Used by `ci-path-applicability.mjs` to decide
// whether a registered revision's CI coverage can be answered without an
// exact-SHA run.
//
// Deliberately narrow: only the literal-list `paths-ignore` form under
// `push`/`pull_request` is supported, and only patterns of the exact shape
// `<prefix>/**`. Aliases, expressions, generated/templated policy, flow
// collections, comments, or any other structure fail closed to
// `{ supported: false }` rather than being approximated. No general YAML
// engine is used or required.

const onKeyPattern = /^(on|"on"|'on'):\s*$/;
const mappingKeyPattern = /^([A-Za-z0-9_-]+):\s*$/;
const sequenceItemPattern = /^-\s+(.+)$/;
const disallowedIndicatorPattern = /^[&*!|>%@`]/;
const disallowedBlockPattern = /[#[\]{}]|\$\{\{|<<:/;
export const supportedIgnoreGlobPattern = /^[^*]+\/\*\*$/;

function parseScalar(raw) {
  const trimmed = raw.trim();
  if (
    trimmed.length >= 2 &&
    ((trimmed[0] === "'" && trimmed.endsWith("'")) ||
      (trimmed[0] === '"' && trimmed.endsWith('"')))
  )
    return trimmed.slice(1, -1);
  if (disallowedIndicatorPattern.test(trimmed)) return null;
  return trimmed;
}

// Groups `blockRaw` lines into mapping entries at exactly `indent`, each with
// its own nested lines as `children`. Returns null (unsupported) for any
// line at `indent` that is not a plain `key:` block-mapping opener, or for
// any line indented less than `indent` (malformed nesting).
function parseMapping(blockRaw, indent) {
  if (blockRaw.some(({ indent: lineIndent }) => lineIndent < indent))
    return null;
  const entries = [];
  for (let i = 0; i < blockRaw.length; i++) {
    const line = blockRaw[i];
    if (line.indent !== indent) continue;
    const match = mappingKeyPattern.exec(line.text);
    if (!match) return null;
    const children = [];
    let j = i + 1;
    while (j < blockRaw.length && blockRaw[j].indent > indent) {
      children.push(blockRaw[j]);
      j++;
    }
    entries.push({ key: match[1], children });
  }
  return entries;
}

function parseEventPolicy(children) {
  if (children.length === 0) return { pathsIgnore: [] };
  const keys = parseMapping(children, children[0].indent);
  if (keys === null || keys.length !== 1 || keys[0].key !== "paths-ignore")
    return null;
  const items = keys[0].children;
  if (items.length === 0) return null;
  const itemIndent = items[0].indent;
  const pathsIgnore = [];
  for (const item of items) {
    if (item.indent !== itemIndent) return null;
    const match = sequenceItemPattern.exec(item.text);
    if (!match) return null;
    const scalar = parseScalar(match[1]);
    if (scalar === null || !supportedIgnoreGlobPattern.test(scalar))
      return null;
    pathsIgnore.push(scalar);
  }
  return { pathsIgnore };
}

// Reads the accepted narrow literal-list `paths-ignore` policy out of a
// GitHub Actions workflow file's text. Returns
// `{ supported: true, events: { push?, pull_request? }, workflowDispatch }`
// when the `on:` block matches the supported narrow form, or
// `{ supported: false }` for anything else (aliases, expressions, flow
// collections, comments, unexpected trigger keys, or unsupported glob
// shapes).
export function readCiPathIgnorePolicy(workflowContent) {
  if (typeof workflowContent !== "string") return { supported: false };
  const lines = workflowContent.split(/\r\n|\r|\n/);
  const onLineIndex = lines.findIndex((line) => onKeyPattern.test(line));
  if (onLineIndex === -1) return { supported: false };

  const blockRaw = [];
  for (let i = onLineIndex + 1; i < lines.length; i++) {
    const line = lines[i];
    if (line.trim() === "") continue;
    if (line.includes("\t")) return { supported: false };
    const indent = line.length - line.trimStart().length;
    if (indent === 0) break;
    blockRaw.push({ indent, text: line.trim() });
  }
  if (blockRaw.length === 0) return { supported: false };
  if (disallowedBlockPattern.test(blockRaw.map(({ text }) => text).join("\n")))
    return { supported: false };

  const topKeys = parseMapping(blockRaw, blockRaw[0].indent);
  if (topKeys === null) return { supported: false };

  const result = { supported: true, workflowDispatch: false, events: {} };
  for (const { key, children } of topKeys) {
    if (key === "workflow_dispatch") {
      result.workflowDispatch = true;
      continue;
    }
    if (key !== "push" && key !== "pull_request") return { supported: false };
    const eventPolicy = parseEventPolicy(children);
    if (eventPolicy === null) return { supported: false };
    result.events[key] = eventPolicy;
  }
  return result;
}
