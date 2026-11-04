// Runs Vitest on fixture projects, each in its own temporary Git checkout with
// its own .planning/open-dough.json, with the recorder configured beside the
// default reporter. Only the inner runs set RECORD_VITEST_PROOF.
import assert from "node:assert/strict"
import { execFileSync, spawn } from "node:child_process"
import { once } from "node:events"
import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readdirSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from "node:fs"
import { tmpdir } from "node:os"
import { dirname, join } from "node:path"
import { test } from "node:test"
import { fileURLToPath } from "node:url"

const frontend = dirname(fileURLToPath(import.meta.url))
const vitest = join(frontend, "node_modules/vitest/vitest.mjs")
const recorder = join(frontend, "vitestProofReporter.ts")

// Writes every finished case's reporter result, as Vitest delivers it, beside
// the recorder in the same run.
const probe = `import { appendFileSync } from "node:fs"
export default class {
  onTestCaseResult(testCase) {
    const names = [testCase.name]
    for (let parent = testCase.parent; parent.type === "suite"; parent = parent.parent) names.unshift(parent.name)
    const { retryCount, duration } = testCase.diagnostic() ?? {}
    appendFileSync(process.env.VITEST_PROOF_PROBE, JSON.stringify({ test: names, project: testCase.project.name, state: testCase.result().state, retryCount, duration }) + "\\n")
  }
}
`

function fixture(t, files, { git: inGit = true, config = {} } = {}) {
  const root = mkdtempSync(join(tmpdir(), "donut-vitest-proof-test-"))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const git = (...args) =>
    execFileSync("git", args, {
      cwd: root,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
    }).trim()
  const write = (file, content) => {
    mkdirSync(dirname(join(root, file)), { recursive: true })
    writeFileSync(join(root, file), content)
  }
  write(
    ".planning/open-dough.json",
    JSON.stringify({
      intent: {
        locations: [
          { path: "web", kind: "typescript", runner: "vitest", proof: "proof" },
          { path: "other", kind: "typescript" },
        ],
      },
    })
  )
  write("probe.mjs", probe)
  for (const [file, content] of Object.entries(files)) write(file, content)
  if (inGit) {
    git("init", "--quiet")
    git("add", ".")
    git(
      "-c",
      "user.name=Proof Fixture",
      "-c",
      "user.email=proof@example.test",
      "commit",
      "--quiet",
      "-m",
      "Starting point"
    )
  }
  let sequence = 0
  function start(
    args = [],
    { record = true, reporters = ["default", recorder], env: extra = {} } = {}
  ) {
    const run = sequence++
    const probeFile = join(root, `probe-${run}.ndjson`)
    const configFile = `vitest-${run}.config.mjs`
    const testConfig = {
      globals: true,
      include: ["web/**/*.spec.ts", "other/**/*.spec.ts"],
      reporters: [...reporters, join(root, "probe.mjs")],
      ...config,
    }
    write(
      configFile,
      `export default ${JSON.stringify({ test: testConfig })}\n`
    )
    const env = { ...process.env, ...extra, VITEST_PROOF_PROBE: probeFile }
    for (const name of ["RECORD_VITEST_PROOF", "CI", "FORCE_COLOR"])
      delete env[name]
    env.NO_COLOR = "1"
    if (record) env.RECORD_VITEST_PROOF = "1"
    const child = spawn(
      process.execPath,
      [vitest, "run", "--config", configFile, ...args],
      { cwd: root, env, stdio: ["ignore", "pipe", "pipe"] }
    )
    t.after(() => {
      if (child.exitCode === null && child.signalCode === null)
        child.kill("SIGKILL")
    })
    let stdout = ""
    let stderr = ""
    child.stdout.setEncoding("utf8").on("data", (chunk) => {
      stdout += chunk
    })
    child.stderr.setEncoding("utf8").on("data", (chunk) => {
      stderr += chunk
    })
    const completed = once(child, "close").then(([code, signal]) => ({
      code,
      signal,
      stdout,
      stderr,
      results: existsSync(probeFile)
        ? readFileSync(probeFile, "utf8")
            .trim()
            .split("\n")
            .filter(Boolean)
            .map((line) => JSON.parse(line))
        : [],
    }))
    return { child, completed }
  }
  const proofFiles = () =>
    existsSync(join(root, "proof"))
      ? readdirSync(join(root, "proof"), { recursive: true }).sort()
      : []
  const snapshot = () => readFileSync(join(root, "proof/latest.json"), "utf8")
  const observations = () => {
    if (!existsSync(join(root, "proof/latest.json"))) return []
    const proof = JSON.parse(snapshot())
    assert.equal(proof.version, 1)
    return proof.observations
  }
  return {
    root,
    git,
    write,
    start,
    proofFiles,
    snapshot,
    observations,
    run: async (...args) => start(...args).completed,
  }
}

function assertRun(result, code = 0) {
  assert.equal(result.signal, null, result.stdout + result.stderr)
  assert.equal(result.code, code, result.stdout + result.stderr)
}

// Vitest's own timings, including its worker-startup hint printed only when
// startup is slow, and the fixture's run-specific names are the only output
// that differs between otherwise identical runs.
const normalized = (output) =>
  output
    .replace(/\n *Isolate .*\n(?: +at least .*\n)?/g, "")
    .replace(/ \d+(\.\d+)?m?s\b/g, " <time>")
    .replace(/^ *(Start at|Duration) .*$/gm, "$1 <time>")
    .replace(/vitest-\d+\.config\.mjs/g, "vitest.config.mjs")

// Spends CPU time inside an attempt so its duration is at least this long.
const spend = (ms) =>
  `{ const until = performance.now() + ${ms}; while (performance.now() < until); }`
const passing = (name) => `it(${JSON.stringify(name)}, () => {})\n`
const failing = (name) =>
  `it(${JSON.stringify(name)}, () => { throw new Error("EXPECTED_FAILURE") })\n`
const key = ({ test, project }) => JSON.stringify([test, project ?? null])
const byKey = (rows) => Object.fromEntries(rows.map((row) => [key(row), row]))

test("a retried pass records one observation with both attempts and their duration", async (t) => {
  const f = fixture(
    t,
    {
      "web/retry.spec.ts": `let attempt = 0\nit("passes on retry", () => { attempt++; ${spend(30)} if (attempt === 1) throw new Error("first attempt") })\n`,
    },
    { config: { retry: 1 } }
  )
  const result = await f.run()
  assertRun(result)
  assert.equal(result.results.length, 1)
  const [delivered] = result.results
  assert.deepEqual([delivered.state, delivered.retryCount], ["passed", 1])
  assert.ok(delivered.duration >= 60, `duration ${delivered.duration}`)
  const [observation] = f.observations()
  assert.equal(observation.attempts, 2)
  assert.equal(observation.durationMs, delivered.duration)
})

test("a recording run records each passing case of covered files with its identity and versions", async (t) => {
  const f = fixture(t, {
    "web/b.spec.ts": passing("second file"),
    "web/a.spec.ts": `describe("Cart", () => { describe("totals", () => { ${passing("zeta")} }) })\n${passing("alpha")}`,
    "other/ignored.spec.ts": passing("not covered"),
  })
  const head = f.git("rev-parse", "HEAD")
  const startedAt = Date.now()
  const result = await f.run()
  assertRun(result)
  assert.deepEqual(f.proofFiles(), ["latest.json"])
  const observations = f.observations()
  assert.deepEqual(
    observations.map(({ file, test }) => [file, test]),
    [
      ["web/a.spec.ts", ["Cart", "totals", "zeta"]],
      ["web/a.spec.ts", ["alpha"]],
      ["web/b.spec.ts", ["second file"]],
    ]
  )
  const delivered = byKey(result.results.map(({ project, ...row }) => row))
  for (const row of observations) {
    assert.deepEqual(Object.keys(row), [
      "file",
      "test",
      "outcome",
      "attempts",
      "durationMs",
      "finishedAt",
      "commit",
      "fileBlob",
    ])
    assert.equal(row.outcome, "passed")
    assert.equal(row.attempts, 1)
    assert.equal(row.durationMs, delivered[key(row)].duration)
    assert.ok(Date.parse(row.finishedAt) >= startedAt)
    assert.ok(row.finishedAt.endsWith("Z"))
    assert.equal(row.commit, head)
    assert.equal(row.fileBlob, f.git("hash-object", row.file))
  }
})

test("without the option a run creates and changes no proof, passing or failing", async (t) => {
  const f = fixture(t, { "web/a.spec.ts": passing("kept") + failing("fails") })
  assertRun(await f.run([], { record: false }), 1)
  assert.deepEqual(f.proofFiles(), [])
  assert.ok(!existsSync(join(f.root, "proof.lock")))
  f.write("web/a.spec.ts", passing("kept"))
  assertRun(await f.run())
  const bytes = f.snapshot()
  f.write("web/a.spec.ts", failing("kept"))
  assertRun(await f.run([], { record: false }), 1)
  assert.equal(f.snapshot(), bytes)
  assert.deepEqual(f.proofFiles(), ["latest.json"])
  assert.ok(!existsSync(join(f.root, "proof.lock")))
})

test("a placeholder table names each row as Vitest formatted it", async (t) => {
  const f = fixture(t, {
    "web/cart.spec.ts": `describe("Cart", () => { it.each([1, 2])("adds %i", () => {}) })\n`,
  })
  assertRun(await f.run())
  assert.deepEqual(
    f.observations().map((row) => row.test),
    [
      ["Cart", "adds 1"],
      ["Cart", "adds 2"],
    ]
  )
})

test("a table without placeholders folds its rows: one failing row clears it, passing rows sum", async (t) => {
  const table = (second) =>
    `it.each([1, ${second}])("rows", (row) => { if (row !== 1) throw new Error("EXPECTED_FAILURE") })\n`
  const f = fixture(t, {
    "web/table.spec.ts": passing("neighbour") + table(1),
  })
  const first = await f.run()
  assertRun(first)
  const rows = first.results.filter((row) => row.test[0] === "rows")
  assert.equal(rows.length, 2)
  const folded = byKey(f.observations())[key({ test: ["rows"] })]
  assert.equal(folded.attempts, 1)
  assert.equal(folded.durationMs, rows[0].duration + rows[1].duration)
  assert.equal(f.observations().length, 2)
  f.write("web/table.spec.ts", passing("neighbour") + table(2))
  assertRun(await f.run(), 1)
  assert.deepEqual(
    f.observations().map((row) => row.test),
    [["neighbour"]]
  )
})

test("a failing case clears only its own proof", async (t) => {
  const f = fixture(t, {
    "web/a.spec.ts": passing("stays") + passing("breaks"),
  })
  assertRun(await f.run())
  const before = byKey(f.observations())
  f.write("web/a.spec.ts", passing("stays") + failing("breaks"))
  assertRun(await f.run(), 1)
  const after = f.observations()
  assert.deepEqual(
    after.map((row) => row.test),
    [["stays"]]
  )
  assert.notEqual(after[0].finishedAt, before[key(after[0])].finishedAt)
})

test("the same file under two projects records one observation per project, and a failure clears only its project's", async (t) => {
  const projects = ["chromium-touch", "chromium"].map((name) => ({
    extends: true,
    test: { name, env: { FIXTURE_PROJECT: name } },
  }))
  const f = fixture(
    t,
    {
      "web/a.spec.ts": `${passing("plain")}it("flaky", () => { if (process.env.FIXTURE_PROJECT === process.env.FAIL_PROJECT) throw new Error("EXPECTED_FAILURE") })\n`,
    },
    { config: { projects } }
  )
  const first = await f.run()
  assertRun(first)
  assert.deepEqual(
    f.observations().map(({ test, project }) => [test, project]),
    [
      [["flaky"], "chromium"],
      [["flaky"], "chromium-touch"],
      [["plain"], "chromium"],
      [["plain"], "chromium-touch"],
    ]
  )
  const before = byKey(f.observations())
  const second = await f.run([], { env: { FAIL_PROJECT: "chromium-touch" } })
  assertRun(second, 1)
  const after = byKey(f.observations())
  assert.deepEqual(Object.keys(after).sort(), [
    key({ test: ["flaky"], project: "chromium" }),
    key({ test: ["plain"], project: "chromium" }),
    key({ test: ["plain"], project: "chromium-touch" }),
  ])
  for (const [identity, row] of Object.entries(after))
    assert.notEqual(row.finishedAt, before[identity].finishedAt)
})

test("skipped, todo, not-focused and filtered-out cases keep their proof", async (t) => {
  const f = fixture(
    t,
    {
      "web/a.spec.ts": passing("skipped") + passing("todo") + passing("run"),
      "web/b.spec.ts": passing("focused") + passing("unfocused"),
    },
    { config: { allowOnly: true } }
  )
  assertRun(await f.run())
  const before = byKey(f.observations())
  f.write(
    "web/a.spec.ts",
    `it.skip("skipped", () => { throw new Error("ran") })\nit.todo("todo")\n${passing("run")}`
  )
  f.write(
    "web/b.spec.ts",
    `it.only("focused", () => {})\n${failing("unfocused")}`
  )
  assertRun(await f.run())
  const after = byKey(f.observations())
  for (const kept of [["skipped"], ["todo"], ["unfocused"]])
    assert.deepEqual(after[key({ test: kept })], before[key({ test: kept })])
  for (const refreshed of [["run"], ["focused"]])
    assert.notEqual(
      after[key({ test: refreshed })].finishedAt,
      before[key({ test: refreshed })].finishedAt
    )
  assertRun(await f.run(["-t", "run"]))
  assert.deepEqual(
    byKey(f.observations())[key({ test: ["focused"] })],
    after[key({ test: ["focused"] })]
  )
})

test("a shard records only the cases it ran", async (t) => {
  const f = fixture(t, {
    "web/a.spec.ts": passing("in a"),
    "web/b.spec.ts": passing("in b"),
  })
  assertRun(await f.run())
  const before = byKey(f.observations())
  const shard = await f.run(["--shard=1/2"])
  assertRun(shard)
  assert.equal(shard.results.length, 1)
  const ran = key({ test: shard.results[0].test })
  const after = byKey(f.observations())
  assert.deepEqual(Object.keys(after), Object.keys(before))
  for (const [identity, row] of Object.entries(after)) {
    if (identity === ran)
      assert.notEqual(row.finishedAt, before[identity].finishedAt)
    else assert.deepEqual(row, before[identity])
  }
})

test("an interrupted recording run keeps completed updates and claims nothing it did not reach", async (t) => {
  const f = fixture(t, {
    "web/a.spec.ts": passing("first") + passing("unreached"),
  })
  assertRun(await f.run())
  const before = byKey(f.observations())
  const first = key({ test: ["first"] })
  f.write(
    "web/a.spec.ts",
    `${passing("first")}it("unreached", () => new Promise(() => {}), 60000)\n`
  )
  // Stop the run once the first case's refresh is visible.
  const run = f.start()
  const polling = setInterval(() => {
    const observed = byKey(f.observations())[first]
    if (observed.finishedAt !== before[first].finishedAt)
      run.child.kill("SIGKILL")
  }, 10)
  t.after(() => clearInterval(polling))
  const result = await run.completed
  clearInterval(polling)
  assert.equal(result.signal, "SIGKILL")
  const after = byKey(f.observations())
  assert.notEqual(after[first].finishedAt, before[first].finishedAt)
  const unreached = key({ test: ["unreached"] })
  assert.deepEqual(after[unreached], before[unreached])
  assert.deepEqual(
    f.proofFiles().filter((name) => name.endsWith(".json")),
    ["latest.json"]
  )
})

test("outside a Git checkout a recording run records nothing and keeps its output and exit", async (t) => {
  const f = fixture(
    t,
    { "web/a.spec.ts": passing("passes") + failing("fails") },
    { git: false }
  )
  const recorded = await f.run()
  const plain = await f.run([], { record: false, reporters: ["default"] })
  assertRun(recorded, 1)
  assertRun(plain, 1)
  assert.equal(normalized(recorded.stdout), normalized(plain.stdout))
  assert.deepEqual(f.proofFiles(), [])
})

test("the loaded recorder, recording or not, keeps output, diagnostics and exit", async (t) => {
  const f = fixture(t, {
    "web/a.spec.ts": passing("passes") + failing("fails"),
    "other/b.spec.ts": passing("elsewhere"),
  })
  // Vitest orders files by its cached results, which the first run creates.
  const serial = ["--no-file-parallelism"]
  const plain = { record: false, reporters: ["default"] }
  assertRun(await f.run(serial, plain), 1)
  const baseline = await f.run(serial, plain)
  assertRun(baseline, 1)
  assert.match(baseline.stderr + baseline.stdout, /EXPECTED_FAILURE/)
  for (const record of [false, true]) {
    const result = await f.run(serial, { record })
    assertRun(result, 1)
    assert.equal(normalized(result.stdout), normalized(baseline.stdout))
    assert.equal(normalized(result.stderr), normalized(baseline.stderr))
  }
  assert.deepEqual(
    f.observations().map((row) => row.test),
    [["passes"]]
  )
})
