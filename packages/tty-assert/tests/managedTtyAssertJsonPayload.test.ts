import { describe, expect, it } from 'vitest'
import {
  managedTtyAssertOptionsFromJson,
  regExpFromSerializable,
} from '../src/managedTtyAssertJsonPayload'

describe('managedTtyAssertJsonPayload', () => {
  it('turns serializable regex fields into RegExp', () => {
    const opts = managedTtyAssertOptionsFromJson({
      surface: 'strippedTranscript',
      needle: { source: 'a+b', flags: 'i' },
      startAfterAnchor: [{ source: '^x' }],
    })
    expect(opts.needle).toBeInstanceOf(RegExp)
    expect((opts.needle as RegExp).source).toBe('a+b')
    expect((opts.needle as RegExp).flags).toContain('i')
    expect(opts.startAfterAnchor).toHaveLength(1)
    expect(opts.startAfterAnchor![0].test('x')).toBe(true)
  })

  it('regExpFromSerializable matches new RegExp(source, flags)', () => {
    const r = regExpFromSerializable({ source: '\\d', flags: 'g' })
    expect([...'a1b2'.matchAll(r)].map((m) => m[0])).toEqual(['1', '2'])
  })
})
