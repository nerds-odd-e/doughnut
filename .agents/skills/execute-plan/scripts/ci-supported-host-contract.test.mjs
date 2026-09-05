import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import * as watcher from './watch-ci.mjs'

const skillRoot = new URL('../', import.meta.url)

function reference(path) {
  return readFileSync(new URL(path, skillRoot), 'utf8')
}

test('supported hosts share one execution observer instead of a per-SHA watcher', () => {
  const contract = [
    reference('SKILL.md'),
    reference('references/wrap-up.md'),
    reference('references/ci-monitor.md'),
    reference('references/ci-notify-hosts.md'),
  ].join('\n')

  assert.equal('watchCi' in watcher, false)
  assert.doesNotMatch(
    contract,
    /FULL_PUSHED_SHA|new observer for the repair SHA/
  )
  assert.match(contract, /one observer per\s+repository\/branch\/coordinator/)
  assert.match(
    contract,
    /reuse(?:s|d)? (?:it|the same observer).*repair push/is
  )
  assert.match(contract, /stop observers without waiting\s+for CI/)
})
