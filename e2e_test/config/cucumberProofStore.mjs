// Successful scenario proof in this checkout. Serialize refreshes against all
// current observations and retain one latest successful snapshot.
import {
  existsSync,
  mkdirSync,
  readdirSync,
  readFileSync,
  readlinkSync,
  renameSync,
  symlinkSync,
  unlinkSync,
  writeFileSync,
} from 'node:fs'
import path from 'node:path'

const identityOf = ({ feature, scenario, example }) =>
  JSON.stringify([feature, scenario, example ?? null])

function observationsAt(file) {
  return JSON.parse(readFileSync(file, 'utf8')).observations
}

function writeAtomically(file, observations) {
  const temporary = `${file}.${process.pid}.tmp`
  try {
    writeFileSync(
      temporary,
      `{\n  \"version\": 1,\n  \"observations\": [\n${observations
        .map((row) => `    ${JSON.stringify(row)}`)
        .join(',\n')}\n  ]\n}\n`
    )
    renameSync(temporary, file)
  } finally {
    if (existsSync(temporary)) unlinkSync(temporary)
  }
}

function acquire(directory) {
  const lock = `${directory}.lock`
  const deadline = Date.now() + 1000
  const waiting = new Int32Array(new SharedArrayBuffer(4))
  for (;;) {
    try {
      // The owner identity and exclusive claim become visible together.
      symlinkSync(String(process.pid), lock)
      return () => unlinkSync(lock)
    } catch (error) {
      if (error.code !== 'EEXIST') throw error
      try {
        const owner = Number(readlinkSync(lock))
        if (owner > 0) process.kill(owner, 0)
      } catch (ownerError) {
        const code = ownerError.code
        if (code === 'ESRCH') {
          try {
            unlinkSync(lock)
          } catch (released) {
            if (released.code !== 'ENOENT') throw released
          }
        } else if (code !== 'ENOENT') throw ownerError
      }
      if (Date.now() >= deadline) throw new Error('Proof directory is busy')
      Atomics.wait(waiting, 0, 0, 10)
    }
  }
}

export function maintainScenarioProof(directory, observation) {
  mkdirSync(directory, { recursive: true })
  const release = acquire(directory)
  try {
    const files = readdirSync(directory, {
      recursive: true,
      encoding: 'utf8',
    })
      .filter((name) => name.endsWith('.json'))
      .map((name) => path.join(directory, name))
    const latest = new Map()
    for (const row of [...files.flatMap(observationsAt), observation]) {
      const identity = identityOf(row)
      const previous = latest.get(identity)
      const difference = previous
        ? Date.parse(row.finishedAt) - Date.parse(previous.finishedAt)
        : 1
      if (
        difference > 0 ||
        (difference === 0 &&
          (row.outcome !== 'passed' || previous.outcome === 'passed'))
      )
        latest.set(identity, row)
    }
    const kept = [...latest.entries()]
      .filter(([, row]) => row.outcome === 'passed')
      .sort(([left], [right]) => (left < right ? -1 : left > right ? 1 : 0))
      .map(([, row]) => row)
    const snapshot = path.join(directory, 'latest.json')
    if (kept.length) writeAtomically(snapshot, kept)
    for (const file of files)
      if (file !== snapshot || !kept.length) unlinkSync(file)
  } finally {
    release()
  }
}
