#!/usr/bin/env node
/**
 * Aggregates timing from e2e_test/timing_log.jsonl (from RECORD_E2E_TIMING=1 runs).
 * Usage: node e2e_test/aggregate_timing.mjs
 */

import { readFileSync, existsSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))
const repoRoot = join(__dirname, '..')
const logPath = join(repoRoot, 'e2e_test', 'timing_log.jsonl')

if (!existsSync(logPath)) {
  console.error('No timing log found at', logPath)
  console.error(
    'Run: RECORD_E2E_TIMING=1 pnpm cypress run --spec e2e_test/features/cli/cli_recall.feature'
  )
  process.exit(1)
}

const lines = readFileSync(logPath, 'utf8').trim().split('\n').filter(Boolean)

const byLabel = {}
for (const line of lines) {
  const { label, duration } = JSON.parse(line)
  if (!byLabel[label]) byLabel[label] = { total: 0, count: 0, samples: [] }
  byLabel[label].total += duration
  byLabel[label].count += 1
  byLabel[label].samples.push(duration)
}

const ms = (n) => `${Math.round(n)}ms`
const sec = (n) => `${(n / 1000).toFixed(1)}s`

console.log('\n## E2E Timing Summary (cli_recall.feature)\n')
console.log('| Label           | Total    | Count | Avg     |')
console.log('|-----------------|----------|-------|---------|')

let grandTotal = 0
const labels = [
  'db-reset',
  'token-nav',
  'token-generateToken',
  'token-cli-add',
  'token-setup',
  'assimilate-note',
  'cli-run',
]
for (const label of labels) {
  const d = byLabel[label]
  if (!d) continue
  grandTotal += d.total
  const avg = d.total / d.count
  console.log(
    `| ${label.padEnd(15)} | ${sec(d.total).padStart(8)} | ${String(d.count).padStart(5)} | ${ms(avg).padStart(7)} |`
  )
}

console.log('|-----------------|----------|-------|---------|')
console.log(
  `| **Total**       | **${sec(grandTotal).padStart(6)}** |       |         |`
)
console.log('')
