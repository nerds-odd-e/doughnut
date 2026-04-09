import { describe, expect, it } from 'vitest'
import { managedTtyAssertOptionsFromJson } from '../src/managedTtyAssertJsonPayload'

describe('managedTtyAssertJsonPayload', () => {
  it('turns JSON regexp fields into RegExp', () => {
    const opts = managedTtyAssertOptionsFromJson({
      surface: 'strippedTranscript',
      needle: { source: 'a+b', flags: 'i' },
      startAfterAnchor: [{ source: '^x' }],
    })
    expect(opts.needle).toBeInstanceOf(RegExp)
    expect((opts.needle as RegExp).source).toBe('a+b')
    expect((opts.needle as RegExp).flags).toContain('i')
    expect(opts.startAfterAnchor).toHaveLength(1)
    const anchor = opts.startAfterAnchor?.[0]
    expect(anchor).toBeDefined()
    expect(anchor!.test('x')).toBe(true)
  })

  it('honors global flag on JSON needle regexp', () => {
    const opts = managedTtyAssertOptionsFromJson({
      surface: 'strippedTranscript',
      needle: { source: '\\d', flags: 'g' },
    })
    const r = opts.needle as RegExp
    expect([...'a1b2'.matchAll(r)].map((m) => m[0])).toEqual(['1', '2'])
  })
})
