import { describe, test, expect } from 'vitest'
import {
  getVersion,
  formatVersionOutput,
  parseVersionFromOutput,
  compareVersions,
} from '../src/version.js'

describe('version', () => {
  test('getVersion returns version string', () => {
    expect(getVersion()).toMatch(/^\d+\.\d+\.\d+$/)
  })

  test('formatVersionOutput returns doughnut prefix with version', () => {
    expect(formatVersionOutput()).toBe(`doughnut ${getVersion()}`)
  })

  test('parseVersionFromOutput extracts version from output', () => {
    expect(parseVersionFromOutput('doughnut 0.2.0')).toBe('0.2.0')
    expect(parseVersionFromOutput('doughnut 0.1.0')).toBe('0.1.0')
    expect(parseVersionFromOutput('other text doughnut 1.2.3 more')).toBe(
      '1.2.3'
    )
  })

  test('parseVersionFromOutput returns null for invalid output', () => {
    expect(parseVersionFromOutput('')).toBeNull()
    expect(parseVersionFromOutput('hello world')).toBeNull()
    expect(parseVersionFromOutput('doughnut')).toBeNull()
  })

  test('compareVersions returns negative when first is less', () => {
    expect(compareVersions('0.1.0', '0.2.0')).toBeLessThan(0)
    expect(compareVersions('0.1.0', '0.1.1')).toBeLessThan(0)
    expect(compareVersions('0.1.0', '1.0.0')).toBeLessThan(0)
  })

  test('compareVersions returns zero when equal', () => {
    expect(compareVersions('0.1.0', '0.1.0')).toBe(0)
  })

  test('compareVersions returns positive when first is greater', () => {
    expect(compareVersions('0.2.0', '0.1.0')).toBeGreaterThan(0)
    expect(compareVersions('0.1.1', '0.1.0')).toBeGreaterThan(0)
    expect(compareVersions('1.0.0', '0.1.0')).toBeGreaterThan(0)
  })
})
