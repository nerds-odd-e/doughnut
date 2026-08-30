/**
 * Authored wiki or path-Markdown link target: note target plus optional `#prop:`
 * encoded property key (ADR 0004). Decode once; invalid escape or UTF-8 is not
 * a property target. Product encode uses uppercase hex; readers accept either
 * hex case.
 */
export type WikiLinkAuthoredTarget = {
  noteTarget: string
  encodedPropertyKey: string | undefined
}

const PROPERTY_SEPARATOR = "#prop:"

const HEX_UPPER = "0123456789ABCDEF"

function isRfc3986Unreserved(byte: number): boolean {
  return (
    (byte >= 65 && byte <= 90) ||
    (byte >= 97 && byte <= 122) ||
    (byte >= 48 && byte <= 57) ||
    byte === 45 ||
    byte === 46 ||
    byte === 95 ||
    byte === 126
  )
}

export function parseWikiLinkAuthoredTarget(
  target: string
): WikiLinkAuthoredTarget {
  const separator = target.indexOf(PROPERTY_SEPARATOR)
  if (separator < 0) {
    return { noteTarget: target, encodedPropertyKey: undefined }
  }
  return {
    noteTarget: target.slice(0, separator),
    encodedPropertyKey: target.slice(separator + PROPERTY_SEPARATOR.length),
  }
}

export function formatWikiLinkAuthoredTarget(
  authored: WikiLinkAuthoredTarget
): string {
  if (authored.encodedPropertyKey === undefined) {
    return authored.noteTarget
  }
  return authored.noteTarget + PROPERTY_SEPARATOR + authored.encodedPropertyKey
}

export function withWikiLinkAuthoredNoteTarget(
  authored: WikiLinkAuthoredTarget,
  newNoteTarget: string
): WikiLinkAuthoredTarget {
  return { ...authored, noteTarget: newNoteTarget }
}

/**
 * Encodes a YAML property key as one `#prop:` component: RFC 3986 unreserved
 * characters stay literal; every other UTF-8 byte is uppercase `%HH`.
 */
export function encodeWikiLinkPropertyKey(yamlKey: string): string {
  const bytes = new TextEncoder().encode(yamlKey)
  let out = ""
  for (const unsigned of bytes) {
    if (isRfc3986Unreserved(unsigned)) {
      out += String.fromCharCode(unsigned)
    } else {
      out += "%"
      out += HEX_UPPER[unsigned >> 4]
      out += HEX_UPPER[unsigned & 0xf]
    }
  }
  return out
}

/**
 * Decodes one `#prop:` component. Product output uses uppercase hex; either hex
 * case is accepted. Invalid escape, empty component, or invalid UTF-8 yields
 * undefined.
 */
export function decodeWikiLinkPropertyKey(
  encoded: string | undefined
): string | undefined {
  if (encoded === undefined || encoded.length === 0) {
    return undefined
  }
  const buf: number[] = []
  const encoder = new TextEncoder()
  for (let i = 0; i < encoded.length; ) {
    const c = encoded.charAt(i)
    if (c === "%") {
      if (i + 2 >= encoded.length) {
        return undefined
      }
      const hi = hexValue(encoded.charAt(i + 1))
      const lo = hexValue(encoded.charAt(i + 2))
      if (hi < 0 || lo < 0) {
        return undefined
      }
      buf.push((hi << 4) | lo)
      i += 3
    } else {
      buf.push(...encoder.encode(c))
      i++
    }
  }
  return decodeUtf8(new Uint8Array(buf))
}

function decodeUtf8(bytes: Uint8Array): string | undefined {
  try {
    return new TextDecoder("utf-8", { fatal: true }).decode(bytes)
  } catch {
    return undefined
  }
}

function hexValue(c: string): number {
  if (c >= "0" && c <= "9") {
    return c.charCodeAt(0) - 48
  }
  if (c >= "A" && c <= "F") {
    return c.charCodeAt(0) - 65 + 10
  }
  if (c >= "a" && c <= "f") {
    return c.charCodeAt(0) - 97 + 10
  }
  return -1
}
