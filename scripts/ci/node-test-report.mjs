// Node test reporter for CI proof: one JSON line per check,
// `{"status":"passed"|"failed"|"other","path":<repo-relative test file>,"seconds"?:n}`.
// Leaf tests are checks; skip and todo count as other. A file that fails to
// load is one failed check. A suite or parent test is a check only when it
// fails for its own reason, untimed because its duration covers its subtests.
import { relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = fileURLToPath(new URL('../..', import.meta.url))

const check = ({ type, data }, isParent) => {
  const failed = type === 'test:fail'
  if (isParent) {
    return failed && data.details.error.failureType !== 'subtestsFailed'
      ? { status: 'failed' }
      : undefined
  }
  return {
    status: data.skip || data.todo ? 'other' : failed ? 'failed' : 'passed',
    seconds: data.details.duration_ms / 1000,
  }
}

export default async function* nodeTestReport(source) {
  // Subtests finish just before their parent, one nesting level deeper.
  const lastNesting = new Map()
  for await (const event of source) {
    if (event.type !== 'test:pass' && event.type !== 'test:fail') continue
    // A test defined in an imported module reports its selected file as entryFile.
    const { entryFile, file, nesting, details } = event.data
    const testFile = entryFile ?? file
    const isParent =
      details.type === 'suite' || lastNesting.get(testFile) === nesting + 1
    lastNesting.set(testFile, nesting)
    const result = check(event, isParent)
    if (result) {
      yield `${JSON.stringify({ status: result.status, path: relative(repoRoot, testFile), seconds: result.seconds })}\n`
    }
  }
}
