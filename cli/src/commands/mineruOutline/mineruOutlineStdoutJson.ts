import type { AttachBookLayoutRequestFull } from 'donut-api'
import type {
  MineruOutlineOk,
  MineruOutlineResult,
} from './mineruOutlineTypes.js'

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v)
}

function validateBookLayoutNode(node: unknown): string | null {
  if (!isRecord(node)) {
    return 'each layout node must be an object'
  }
  if (typeof node.title !== 'string' || node.title.trim() === '') {
    return 'each layout node needs a non-empty title'
  }
  if (node.children !== undefined) {
    if (!Array.isArray(node.children)) {
      return 'layout node children must be an array'
    }
    for (const child of node.children) {
      const err = validateBookLayoutNode(child)
      if (err !== null) {
        return err
      }
    }
  }
  return null
}

function parseContentListFromStdoutJson(
  raw: Record<string, unknown>
): { contentList: unknown[] } | { error: string } | { omit: true } {
  if (!('contentList' in raw)) {
    return { omit: true }
  }
  const cl = raw.contentList
  if (!Array.isArray(cl)) {
    return { error: 'contentList must be an array' }
  }
  if (cl.length === 0) {
    return { error: 'contentList must be a non-empty array' }
  }
  for (const item of cl) {
    if (!isRecord(item)) {
      return { error: 'each contentList item must be an object' }
    }
  }
  return { contentList: cl }
}

function parseBookLayoutFromStdoutJson(
  raw: Record<string, unknown>
):
  | { bookLayout: AttachBookLayoutRequestFull }
  | { error: string }
  | { omit: true } {
  if (!('bookLayout' in raw)) {
    return { omit: true }
  }
  const bookLayoutRaw = raw.bookLayout
  if (!isRecord(bookLayoutRaw)) {
    return { error: 'bookLayout must be an object' }
  }
  const roots = bookLayoutRaw.roots
  if (!Array.isArray(roots) || roots.length === 0) {
    return { error: 'bookLayout.roots must be a non-empty array' }
  }
  for (const root of roots) {
    const err = validateBookLayoutNode(root)
    if (err !== null) {
      return { error: err }
    }
  }
  return { bookLayout: bookLayoutRaw as AttachBookLayoutRequestFull }
}

function parseAttachPayloadFromStdoutJson(
  raw: Record<string, unknown>
):
  | { bookLayout: AttachBookLayoutRequestFull }
  | { contentList: unknown[] }
  | { error: string }
  | { omit: true } {
  const contentListParsed = parseContentListFromStdoutJson(raw)
  const bookLayoutParsed = parseBookLayoutFromStdoutJson(raw)
  const hasContentListKey = 'contentList' in raw
  const hasBookLayoutKey = 'bookLayout' in raw

  if (hasContentListKey && hasBookLayoutKey) {
    const contentListOk = 'contentList' in contentListParsed
    const bookLayoutOk = 'bookLayout' in bookLayoutParsed
    if (contentListOk && bookLayoutOk) {
      return {
        error: 'cannot send both bookLayout and contentList in outline JSON',
      }
    }
  }

  if (hasContentListKey && 'error' in contentListParsed) {
    return contentListParsed
  }
  if (hasBookLayoutKey && 'error' in bookLayoutParsed) {
    return bookLayoutParsed
  }
  if ('contentList' in contentListParsed) {
    return contentListParsed
  }
  if ('bookLayout' in bookLayoutParsed) {
    return bookLayoutParsed
  }
  return { omit: true }
}

export function toMineruResult(raw: unknown): MineruOutlineResult | null {
  if (!isRecord(raw)) {
    return null
  }
  if (raw.ok === true) {
    const outline = typeof raw.outline === 'string' ? raw.outline.trim() : ''
    const source = typeof raw.source === 'string' ? raw.source : ''
    const note = typeof raw.note === 'string' ? raw.note : undefined
    const attachParsed = parseAttachPayloadFromStdoutJson(raw)
    if ('error' in attachParsed) {
      return { ok: false, error: attachParsed.error }
    }
    const out: MineruOutlineOk = { ok: true, outline, source }
    if (note !== undefined) {
      out.note = note
    }
    if ('bookLayout' in attachParsed) {
      out.bookLayout = attachParsed.bookLayout
    }
    if ('contentList' in attachParsed) {
      out.contentList = attachParsed.contentList
    }
    return out
  }
  if (raw.ok === false && typeof raw.error === 'string') {
    return { ok: false, error: raw.error }
  }
  return null
}
