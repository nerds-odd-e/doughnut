import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, test, expect } from 'vitest'
import {
  getVersion,
  parseVersionFromOutput,
  compareVersions,
} from '../src/commands/version.js'

const packageVersion = (
  JSON.parse(
    readFileSync(
      join(dirname(fileURLToPath(import.meta.url)), '../package.json'),
      'utf8'
    )
  ) as { version: string }
).version

describe('version', () => {
  test('default version matches cli/package.json', () => {
    expect(getVersion()).toBe(packageVersion)
    expect(getVersion()).toMatch(/^\d+\.\d+\.\d+$/)
  })

  test.each([
    ['doughnut 0.2.0', '0.2.0'],
    ['other text doughnut 1.2.3 more', '1.2.3'],
  ])('parseVersionFromOutput extracts from %j', (output, expected) => {
    expect(parseVersionFromOutput(output)).toBe(expected)
  })

  test.each(['', 'hello world', 'doughnut'])(
    'parseVersionFromOutput returns null for %j',
    (output) => {
      expect(parseVersionFromOutput(output)).toBeNull()
    }
  )

  test.each([
    ['0.1.0', '0.2.0', 'less'],
    ['0.1.0', '0.1.0', 'equal'],
    ['0.2.0', '0.1.0', 'greater'],
  ] as const)('compareVersions %s vs %s is %s', (a, b, rel) => {
    const cmp = compareVersions(a, b)
    if (rel === 'less') expect(cmp).toBeLessThan(0)
    else if (rel === 'equal') expect(cmp).toBe(0)
    else expect(cmp).toBeGreaterThan(0)
  })
})
