import { describe, test, expect } from 'vitest'
import { formatVersionOutput } from '../src/version.js'

describe('CLI', () => {
  test('version command outputs doughnut prefix with version', () => {
    const output = formatVersionOutput()
    expect(output).toMatch(/^doughnut \d+\.\d+\.\d+$/)
  })
})
