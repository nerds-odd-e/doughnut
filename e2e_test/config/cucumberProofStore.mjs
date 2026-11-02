// Successful scenario proof in this checkout. Serialize writers, read their
// current files rather than run snapshots, and remove only the failed identity.
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
      `${JSON.stringify({ version: 1, observations }, null, 2)}\n`
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

export function maintainScenarioProof(directory, runName, observation) {
  mkdirSync(directory, { recursive: true })
  const release = acquire(directory)
  try {
    const identity = identityOf(observation)
    if (observation.outcome === 'passed') {
      const file = path.join(directory, runName)
      const rows = existsSync(file) ? observationsAt(file) : []
      const previous = rows.find((row) => identityOf(row) === identity)
      if (
        previous &&
        Date.parse(previous.finishedAt) > Date.parse(observation.finishedAt)
      )
        return
      writeAtomically(file, [
        ...rows.filter((row) => identityOf(row) !== identity),
        observation,
      ])
      return
    }
    for (const name of readdirSync(directory, {
      recursive: true,
      encoding: 'utf8',
    })) {
      if (!name.endsWith('.json')) continue
      const file = path.join(directory, name)
      const rows = observationsAt(file)
      const kept = rows.filter(
        (row) =>
          identityOf(row) !== identity ||
          Date.parse(row.finishedAt) > Date.parse(observation.finishedAt)
      )
      if (kept.length === rows.length) continue
      if (kept.length) writeAtomically(file, kept)
      else unlinkSync(file)
    }
  } finally {
    release()
  }
}
