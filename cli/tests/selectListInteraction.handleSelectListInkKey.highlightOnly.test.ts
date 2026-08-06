import { describe, expect, it, vi } from 'vitest'
import { handleSelectListInkKey } from '../src/interactions/selectListInteraction.js'
import { highlightOnly } from './selectListInteraction.testHelpers.js'

describe('handleSelectListInkKey (highlight-only)', () => {
  it('up arrow moves highlight', () => {
    const onSetHighlightIndex = vi.fn()
    handleSelectListInkKey(
      '',
      { upArrow: true },
      '',
      1,
      3,
      highlightOnly,
      'abort-list',
      { onSetHighlightIndex, onSubmitHighlightIndex: vi.fn() }
    )
    expect(onSetHighlightIndex).toHaveBeenCalledWith(0)
  })

  it('Enter submits highlight index', () => {
    const onSubmitHighlightIndex = vi.fn()
    handleSelectListInkKey(
      '\r',
      { return: true },
      '',
      2,
      3,
      highlightOnly,
      'abort-list',
      { onSetHighlightIndex: vi.fn(), onSubmitHighlightIndex }
    )
    expect(onSubmitHighlightIndex).toHaveBeenCalledWith(2)
  })

  it('Escape with abort-list calls onAbortHighlightOnlyList', () => {
    const onAbortHighlightOnlyList = vi.fn()
    handleSelectListInkKey(
      '\u001b',
      {},
      '',
      0,
      2,
      highlightOnly,
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onAbortHighlightOnlyList,
      }
    )
    expect(onAbortHighlightOnlyList).toHaveBeenCalledOnce()
  })

  it('Escape with signal-escape calls onEscapeSignaled', () => {
    const onEscapeSignaled = vi.fn()
    handleSelectListInkKey(
      '\u001b',
      {},
      '',
      0,
      2,
      highlightOnly,
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onEscapeSignaled,
      }
    )
    expect(onEscapeSignaled).toHaveBeenCalledOnce()
  })

  it('down arrow moves highlight', () => {
    const onSetHighlightIndex = vi.fn()
    handleSelectListInkKey(
      '',
      { downArrow: true },
      '',
      1,
      3,
      highlightOnly,
      'abort-list',
      { onSetHighlightIndex, onSubmitHighlightIndex: vi.fn() }
    )
    expect(onSetHighlightIndex).toHaveBeenCalledWith(2)
  })

  it('shift+Enter aborts list', () => {
    const onAbortHighlightOnlyList = vi.fn()
    handleSelectListInkKey(
      '\r',
      { return: true, shift: true },
      '',
      0,
      2,
      highlightOnly,
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onAbortHighlightOnlyList,
      }
    )
    expect(onAbortHighlightOnlyList).toHaveBeenCalledOnce()
  })

  it('backspace aborts', () => {
    const onAbortHighlightOnlyList = vi.fn()
    handleSelectListInkKey(
      '',
      { backspace: true },
      '',
      0,
      2,
      highlightOnly,
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onAbortHighlightOnlyList,
      }
    )
    expect(onAbortHighlightOnlyList).toHaveBeenCalledOnce()
  })

  it('typed char aborts', () => {
    const onAbortHighlightOnlyList = vi.fn()
    handleSelectListInkKey('a', {}, '', 0, 2, highlightOnly, 'abort-list', {
      onSetHighlightIndex: vi.fn(),
      onSubmitHighlightIndex: vi.fn(),
      onAbortHighlightOnlyList,
    })
    expect(onAbortHighlightOnlyList).toHaveBeenCalledOnce()
  })

  it('unhandled key aborts', () => {
    const onAbortHighlightOnlyList = vi.fn()
    handleSelectListInkKey(
      '',
      { name: 'f1' },
      '',
      0,
      2,
      highlightOnly,
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onAbortHighlightOnlyList,
      }
    )
    expect(onAbortHighlightOnlyList).toHaveBeenCalledOnce()
  })
})
