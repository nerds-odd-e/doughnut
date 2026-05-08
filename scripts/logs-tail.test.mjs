import assert from 'node:assert'
import path from 'node:path'
import { test } from 'node:test'
import { runLogsTail } from './logs-tail.mjs'

function makeWritable() {
  let content = ''
  return {
    write(chunk) {
      content += chunk
    },
    content() {
      return content
    },
  }
}

test('runLogsTail reports an unknown target without throwing', async () => {
  const out = makeWritable()
  const err = makeWritable()

  const code = await runLogsTail({ argv: ['unknown'], out, err })

  assert.strictEqual(code, 1)
  assert.strictEqual(out.content(), '')
  assert.match(err.content(), /Unknown log target/)
  assert.match(err.content(), /Usage: pnpm logs:tail/)
})

test('runLogsTail treats a missing known log as an empty safe tail', async () => {
  const out = makeWritable()
  const err = makeWritable()

  const code = await runLogsTail({
    argv: ['backend-e2e'],
    out,
    err,
    resolveTarget: () => path.resolve('/tmp/doughnut-missing-test.log'),
  })

  assert.strictEqual(code, 0)
  assert.strictEqual(out.content(), '')
  assert.match(err.content(), /Log file not found/)
})
