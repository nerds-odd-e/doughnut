import { describe, expect, test } from 'vitest'
import { unzipToEntries } from '../src/sync/unzip.js'
import { buildZip } from './zipFixture.js'

describe('unzipToEntries', () => {
  test('reads a deflated entry', () => {
    const zip = buildZip([{ name: 'less.md', content: '# less\n\nHello' }])

    expect(unzipToEntries(zip)).toEqual(
      new Map([['less.md', '# less\n\nHello']])
    )
  })

  test('reads an entry that was stored rather than deflated', () => {
    const zip = buildZip([{ name: 'less.md', content: 'Hello', stored: true }])

    expect(unzipToEntries(zip)).toEqual(new Map([['less.md', 'Hello']]))
  })

  test('reads entries in folders under their full path', () => {
    const zip = buildZip([
      { name: 'intro.md', content: 'Hello' },
      { name: 'LeSS in Action/team.md', content: 'Sprint' },
      { name: 'Engineering/deep/tech.md', content: 'Trunk' },
    ])

    expect([...unzipToEntries(zip).keys()]).toEqual([
      'intro.md',
      'LeSS in Action/team.md',
      'Engineering/deep/tech.md',
    ])
  })

  test('reads an empty entry as empty content', () => {
    const zip = buildZip([{ name: 'less.md', content: '' }])

    expect(unzipToEntries(zip).get('less.md')).toBe('')
  })

  test('reads content that is not ascii', () => {
    const zip = buildZip([{ name: 'less.md', content: '# 中文\n\nこんにちは' }])

    expect(unzipToEntries(zip).get('less.md')).toBe('# 中文\n\nこんにちは')
  })

  test('reads an empty zip as no entries', () => {
    expect(unzipToEntries(buildZip([]))).toEqual(new Map())
  })

  test('rejects bytes that are not a zip', () => {
    expect(() => unzipToEntries(Buffer.from('not a zip at all'))).toThrow(
      'The export was not a readable zip.'
    )
  })
})
