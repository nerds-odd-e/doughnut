// The established preparation a caller hands to a refinement session once it
// has already run the preparation start: the instruction text that names the
// assignment and its workspace, in one fixed order. Optional fields the caller
// does not have are omitted, never blank.
const required = [
  ["identity", "identity"],
  ["workspace", "workspace"],
  ["branch", "branch"],
  ["remote", "remote"],
  ["target", "target"],
  ["agent", "agent"],
];
const optional = [
  ["publishedSha", "publishedSha"],
  ["integration", "integration checkout"],
];

export function formatEstablishedPreparation(preparation) {
  const lines = [
    ...required,
    ...optional.filter(([key]) => preparation[key]),
  ].map(([key, label]) => `- ${label}: ${preparation[key]}`);
  return ["Established preparation:", ...lines].join("\n");
}
