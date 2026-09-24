// Who holds Taken work: the agent name rotation, the agent's Git identity, and
// its published profile under the backlog directory. Profile spelling has one
// owner here. No filesystem, Git, or Node-only imports.

// Fixed rotation, in order. A prime count keeps the cycle even.
export const agentNames = Object.freeze([
  "Yui",
  "Akiho",
  "Yuma",
  "Sola",
  "Yua",
  "Ai",
  "Kirara",
  "Mana",
  "Tsubomi",
  "Yumi",
  "Julia",
  "Tsukasa",
  "Kaoru",
  "Nao",
  "Maria",
  "Mihiro",
  "Aino",
  "Rio",
  "Airi",
  "Shunka",
  "Eimi",
  "Hitomi",
  "Hibiki",
  "Maki",
  "Nana",
  "Honoka",
  "Anri",
  "Koharu",
  "Rina",
]);

// Hosts an agent may report, and the execution modes a Take records. The
// `const` annotations keep each list's literal values for typed readers.
export const agentHosts = Object.freeze(
  /** @type {const} */ (["claude", "codex", "cursor"]),
);
export const agentModes = Object.freeze(
  /** @type {const} */ (["trunk", "story-branch"]),
);

// Profiles live beside the backlog, one file per active agent.
export const agentProfileDirectory = "agents";

export function agentIdentity(name) {
  if (!agentNames.includes(name))
    throw new Error(`unknown agent name: ${name}`);
  const lower = name.toLowerCase();
  return {
    name,
    agent: `${name}-chan`,
    email: `${lower}-chan@example.org`,
    path: `${agentProfileDirectory}/${lower}-chan.json`,
  };
}

// The rotation name a profile file belongs to, or undefined when the file name
// is not an agent profile.
export function profileAgentName(fileName) {
  const path = `${agentProfileDirectory}/${fileName}`;
  return agentNames.find((name) => agentIdentity(name).path === path);
}

// The first name after `mostRecent` (the profile most recently added on trunk,
// even if since released) that is not held, wrapping after Rina to Yui. With no
// profile ever added, rotation starts at Yui. Undefined when all are held.
export function selectAgentName(mostRecent, held) {
  const start = agentNames.indexOf(mostRecent) + 1;
  for (let offset = 0; offset < agentNames.length; offset += 1) {
    const name = agentNames[(start + offset) % agentNames.length];
    if (!held.includes(name)) return name;
  }
  return undefined;
}

// Host and model are what the agent reports; either may be unrecorded. Returns
// why a reported value cannot be recorded, or undefined when both can.
export function agentReportError({ host, model }) {
  if (host !== undefined && !agentHosts.includes(host))
    return `host must be one of ${agentHosts.join(", ")}`;
  if (model !== undefined && (typeof model !== "string" || model.trim() === ""))
    return "model must be non-empty text when recorded";
  return undefined;
}

const nonEmptyText = (value) => typeof value === "string" && value !== "";

// Why a profile's work facts cannot be recorded, or undefined when they can.
// Rendering and reading a profile apply the same rules.
function profileFactsError({ identity, mode, branch, host, model }) {
  if (!nonEmptyText(identity))
    return "agent profile requires a work item identity";
  if (!agentModes.includes(mode)) return `unknown execution mode: ${mode}`;
  if (!nonEmptyText(branch)) return "agent profile requires branch context";
  return agentReportError({ host, model });
}

export function renderAgentProfile({
  name,
  identity,
  mode,
  branch,
  host,
  model,
}) {
  const { agent, email } = agentIdentity(name);
  const factsError = profileFactsError({ identity, mode, branch, host, model });
  if (factsError) throw new Error(factsError);
  const profile = {
    schemaVersion: 1,
    agent,
    email,
    identity,
    mode,
    branch,
    ...(host === undefined ? {} : { host }),
    ...(model === undefined ? {} : { model }),
  };
  return `${JSON.stringify(profile, null, 2)}\n`;
}

// Reads published profile text back into the facts renderAgentProfile takes.
// Returns { ok: true, profile } or { ok: false, error } when the text is not a
// readable profile; unrecorded host and model stay absent.
export function parseAgentProfile(text) {
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    return { ok: false, error: "profile is not JSON" };
  }
  if (data === null || typeof data !== "object" || data.schemaVersion !== 1)
    return { ok: false, error: "profile schemaVersion must be 1" };
  const name = agentNames.find(
    (each) => agentIdentity(each).agent === data.agent,
  );
  if (!name) return { ok: false, error: `unknown agent: ${data.agent}` };
  if (data.email !== agentIdentity(name).email)
    return { ok: false, error: `email does not belong to ${data.agent}` };
  const { identity, mode, branch, host, model } = data;
  const factsError = profileFactsError({ identity, mode, branch, host, model });
  if (factsError) return { ok: false, error: factsError };
  return {
    ok: true,
    profile: {
      name,
      identity,
      mode,
      branch,
      ...(host === undefined ? {} : { host }),
      ...(model === undefined ? {} : { model }),
    },
  };
}
