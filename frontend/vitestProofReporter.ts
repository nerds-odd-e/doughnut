// Maintains each Vitest test case's last successful committed proof, in the
// format of Open Dough's dough-test-optimization vitest-proof reference. Each
// Intent location in .planning/open-dough.json that declares
// `runner: "vitest"` and a `proof` directory receives a stable snapshot for
// the test cases of its files when RECORD_VITEST_PROOF=1; other files are not
// recorded. Vitest runs this reporter once per run in its Node process, for
// every project and browser instance.
import { execFileSync } from "node:child_process"
import { readFileSync, realpathSync } from "node:fs"
import path from "node:path"
import type { Reporter, TestCase, Vitest } from "vitest/node"
import { refreshProof, textOrder } from "../e2e_test/config/proofStore.mjs"

type Observation = {
  file: string
  test: string[]
  project?: string
  outcome: "passed" | "failed"
  attempts: number
  durationMs: number
  finishedAt: string
  commit: string
  fileBlob: string
}

type Location = { files: string; proof: string }

type Source = { file: string; location?: Location; fileBlob?: string }

type Run = {
  root: string
  commit: string
  locations: Location[]
  sources: Map<string, Source>
  executions: Map<string, Observation | "failed">
}

function namesOrder(left: string[], right: string[]): number {
  for (let index = 0; index < Math.min(left.length, right.length); index++) {
    const order = textOrder(left[index], right[index])
    if (order) return order
  }
  return left.length - right.length
}

// A case without a project comes first.
const projectOrder = (left?: string, right?: string) =>
  left === undefined || right === undefined
    ? Number(left !== undefined) - Number(right !== undefined)
    : textOrder(left, right)

const identityOf = ({ file, test, project }: Observation) =>
  JSON.stringify([file, test, project ?? null])

const vitestFormat = {
  identityOf,
  compare: (left: Observation, right: Observation) =>
    textOrder(left.file, right.file) ||
    namesOrder(left.test, right.test) ||
    projectOrder(left.project, right.project),
}

function git(args: string[], cwd: string): string {
  return execFileSync("git", args, {
    cwd,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "ignore"],
  }).trim()
}

function startRun(cwd: string): Run {
  const root = git(["rev-parse", "--show-toplevel"], cwd)
  const settings = JSON.parse(
    readFileSync(path.join(root, ".planning/open-dough.json"), "utf8")
  ) as {
    intent: { locations: { path: string; runner?: string; proof?: string }[] }
  }
  return {
    root,
    commit: git(["rev-parse", "HEAD"], root),
    locations: settings.intent.locations.flatMap((location) =>
      location.runner === "vitest" && location.proof
        ? [{ files: `${location.path}/`, proof: location.proof }]
        : []
    ),
    sources: new Map(),
    executions: new Map(),
  }
}

function namesOf(testCase: TestCase): string[] {
  const names = [testCase.name]
  for (
    let parent = testCase.parent;
    parent.type === "suite";
    parent = parent.parent
  )
    names.unshift(parent.name)
  return names
}

function sourceOf(run: Run, moduleId: string): Source {
  let source = run.sources.get(moduleId)
  if (!source) {
    const file = path
      .relative(run.root, realpathSync(moduleId))
      .split(path.sep)
      .join("/")
    const location = run.locations.find(({ files }) => file.startsWith(files))
    source = {
      file,
      location,
      fileBlob: location && git(["hash-object", "--", file], run.root),
    }
    run.sources.set(moduleId, source)
  }
  return source
}

function record(run: Run, testCase: TestCase): void {
  const state = testCase.result().state
  if (state !== "passed" && state !== "failed") return
  const { file, location, fileBlob } = sourceOf(run, testCase.module.moduleId)
  if (!location || !fileBlob) return
  const diagnostic = testCase.diagnostic()
  const project = testCase.project.name
  const execution: Observation = {
    file,
    test: namesOf(testCase),
    ...(project ? { project } : {}),
    outcome: state,
    attempts: (diagnostic?.retryCount ?? 0) + 1,
    durationMs: diagnostic?.duration ?? 0,
    finishedAt: new Date().toISOString(),
    commit: run.commit,
    fileBlob,
  }
  // Executions sharing an identity in one run, such as the rows of a table
  // without placeholders, fold into one observation: any failure clears it
  // for the rest of the run; passes keep the most attempts and sum durations.
  const identity = identityOf(execution)
  const earlier = run.executions.get(identity)
  if (earlier === "failed") return
  const folded =
    state === "failed"
      ? "failed"
      : earlier
        ? {
            ...execution,
            attempts: Math.max(earlier.attempts, execution.attempts),
            durationMs: earlier.durationMs + execution.durationMs,
          }
        : execution
  run.executions.set(identity, folded)
  refreshProof(
    vitestFormat,
    path.join(run.root, location.proof),
    folded === "failed" ? execution : folded
  )
}

export default class VitestProofReporter implements Reporter {
  private root = process.cwd()
  private run: Run | undefined

  onInit(vitest: Vitest): void {
    this.root = vitest.config.root
  }

  onTestRunStart(): void {
    if (process.env.RECORD_VITEST_PROOF !== "1") return
    this.run = undefined
    try {
      this.run = startRun(this.root)
    } catch {
      // Proof is best effort beside the run: without Git or usable settings
      // the run continues unrecorded and unchanged.
    }
  }

  onTestCaseResult(testCase: TestCase): void {
    if (process.env.RECORD_VITEST_PROOF !== "1" || !this.run) return
    try {
      record(this.run, testCase)
    } catch {
      // A recording failure must not change the run's output or exit, so the
      // run continues and this case stays unrecorded.
    }
  }
}
