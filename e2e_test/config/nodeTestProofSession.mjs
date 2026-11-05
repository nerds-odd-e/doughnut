// Session that turns node --test reporter events into proofStore refreshes.
// Holds children until their parent type is known so suite cases record and
// nested subtests do not.
import { execFileSync } from 'node:child_process'
import { realpathSync } from 'node:fs'
import path from 'node:path'
import { namesOrder, refreshProof, textOrder } from './proofStore.mjs'

const identityOf = ({ file, test }) => JSON.stringify([file, test])

const nodeTestFormat = {
  identityOf,
  compare: (left, right) =>
    textOrder(left.file, right.file) || namesOrder(left.test, right.test),
}

function git(args, cwd) {
  return execFileSync('git', args, {
    cwd,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'ignore'],
  }).trim()
}

function fileKey(file) {
  return file ? path.resolve(file) : ''
}

/**
 * A case is a finished `details.type === "test"` whose enclosing results are
 * suites or the file. Children that finish before their parent are held until
 * the parent type is known: under a suite they are cases; under a test they
 * are subtests and record nothing.
 */
export function createSession(run) {
  const openByFile = new Map()
  const typeByTestId = new Map()
  const pendingByParent = new Map()
  const folds = new Map()
  const blobs = new Map()

  function stackFor(file) {
    const key = fileKey(file)
    let stack = openByFile.get(key)
    if (!stack) {
      stack = []
      openByFile.set(key, stack)
    }
    return stack
  }

  function relativeFileOf(absoluteFile) {
    let resolved
    try {
      resolved = realpathSync(absoluteFile)
    } catch {
      resolved = path.resolve(absoluteFile)
    }
    const relative = path.relative(run.root, resolved).split(path.sep).join('/')
    if (
      relative.length === 0 ||
      relative.startsWith('..') ||
      path.isAbsolute(relative)
    ) {
      return
    }
    return relative
  }

  function locationOf(relativeFile) {
    return run.locations.find(({ files }) => relativeFile.startsWith(files))
  }

  function fileBlobOf(relativeFile) {
    const cached = blobs.get(relativeFile)
    if (cached !== undefined) return cached
    const hash = git(['hash-object', '--', relativeFile], run.root)
    blobs.set(relativeFile, hash)
    return hash
  }

  function suitePathOf(file, nesting, name) {
    const stack = stackFor(file)
    return [...stack.slice(0, nesting).map((entry) => entry.name), name]
  }

  function applyCase(event, kind) {
    if (!event.file) return
    const relativeFile = relativeFileOf(event.file)
    if (relativeFile === undefined) return
    const location = locationOf(relativeFile)
    if (!location) return
    const testNames = suitePathOf(event.file, event.nesting, event.name)
    const identity = { file: relativeFile, test: testNames }
    const foldKey = identityOf(identity)
    const fold = folds.get(foldKey) ?? { cleared: false }

    if (kind === 'keep') return

    if (kind === 'clear') {
      fold.cleared = true
      delete fold.observation
      folds.set(foldKey, fold)
      refreshProof(nodeTestFormat, path.join(run.root, location.proof), {
        ...identity,
        outcome: 'failed',
        attempts: 1,
        durationMs: 0,
        finishedAt: new Date().toISOString(),
        commit: run.commit,
      })
      return
    }

    if (fold.cleared) return

    const durationMs = event.details?.duration_ms ?? 0
    const finishedAt = new Date().toISOString()
    if (fold.observation === undefined) {
      fold.observation = {
        ...identity,
        outcome: 'passed',
        attempts: 1,
        durationMs,
        finishedAt,
        commit: run.commit,
        fileBlob: fileBlobOf(relativeFile),
      }
    } else {
      fold.observation = {
        ...fold.observation,
        durationMs: fold.observation.durationMs + durationMs,
        finishedAt,
      }
    }
    folds.set(foldKey, fold)
    refreshProof(
      nodeTestFormat,
      path.join(run.root, location.proof),
      fold.observation
    )
  }

  function classify(event) {
    if (event.skip || event.todo) return 'keep'
    if (event.type === 'test:pass') return 'pass'
    return 'clear'
  }

  function handleCaseEvent(event) {
    applyCase(event, classify(event))
  }

  function flushPending(parentId, parentType) {
    const pending = pendingByParent.get(parentId)
    pendingByParent.delete(parentId)
    if (!pending || parentType !== 'suite') return
    for (const child of pending) handleCaseEvent(child)
  }

  function onStart(data) {
    const stack = stackFor(data.file)
    stack.length = data.nesting
    stack.push({
      name: data.name,
      testId: data.testId,
      parentId: data.parentId,
    })
  }

  function onFinished(type, data) {
    const detailsType = data.details?.type
    if (detailsType !== 'test' && detailsType !== 'suite') return

    typeByTestId.set(data.testId, detailsType)
    flushPending(data.testId, detailsType)

    if (detailsType !== 'test') return

    const event = { type, ...data }
    const parentType = typeByTestId.get(data.parentId)
    if (parentType === 'test') return
    if (
      parentType === 'suite' ||
      data.parentId === 0 ||
      data.parentId === null ||
      data.parentId === undefined
    ) {
      handleCaseEvent(event)
      return
    }
    const pending = pendingByParent.get(data.parentId) ?? []
    pending.push(event)
    pendingByParent.set(data.parentId, pending)
  }

  return { onStart, onFinished }
}
