// The established start a caller hands to an execution session once it has
// already run the start command: the instruction text that names the claim,
// workspace and published revision, in one fixed order. Optional fields the
// caller does not have are omitted, never blank.
const required = [
  ["identity", "identity"],
  ["publisherId", "publisher ID"],
  ["workspace", "workspace"],
  ["branch", "branch"],
  ["mode", "mode"],
  ["remote", "remote"],
  ["target", "target"],
  ["publishedSha", "publishedSha"],
];
const optional = [
  ["agent", "agent"],
  ["plan", "plan"],
  ["startingRevision", "startingRevision"],
  ["candidateSha", "candidateSha"],
];

export function formatEstablishedStart(start) {
  const lines = [...required, ...optional.filter(([key]) => start[key])].map(
    ([key, label]) => `- ${label}: ${start[key]}`,
  );
  return ["Established start:", ...lines].join("\n");
}
