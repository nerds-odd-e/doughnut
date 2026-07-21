import { describe, expect, it } from "vitest"

/**
 * Source scan via Vite raw glob (browser Vitest cannot use node:fs).
 * Keys look like "../../src/managedApi/clientSetup.ts".
 */
const sources = import.meta.glob("../../src/**/*.{ts,vue}", {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>

const CANCELABLE_TRUE = /cancelable\s*:\s*true/g
const NEW_ABORT_CONTROLLER = /new\s+AbortController\b/g
/** AbortError-name matching (e.g. error.name === "AbortError"), not bare identifiers. */
const ABORT_ERROR_NAME_MATCH =
  /(?:\.name\s*===?\s*['"]AbortError['"]|['"]AbortError['"]\s*===?\s*\w+\.name)/g

const ALLOWED_CANCELABLE_FILES = new Set([
  "components/recall/NoteRefinement.vue",
  "managedApi/clientSetup.ts",
])

function toSrcRel(globKey: string): string {
  const marker = "/src/"
  const idx = globKey.replaceAll("\\", "/").lastIndexOf(marker)
  if (idx === -1) {
    throw new Error(`Unexpected glob key (no /src/): ${globKey}`)
  }
  return globKey.slice(idx + marker.length)
}

function hitsFor(pattern: RegExp): { rel: string; count: number }[] {
  return Object.entries(sources)
    .map(([key, content]) => {
      // Clone so /g lastIndex does not leak across files.
      const matches = content.match(new RegExp(pattern.source, pattern.flags))
      return { rel: toSrcRel(key), count: matches?.length ?? 0 }
    })
    .filter((h) => h.count > 0)
}

describe("cancelable allowlist", () => {
  it("restricts cancelable: true to NoteRefinement + clientSetup", () => {
    const hits = hitsFor(CANCELABLE_TRUE)
    const files = hits.map((h) => h.rel).sort()

    expect(files).toEqual([...ALLOWED_CANCELABLE_FILES].sort())

    const noteRefinement = hits.find((h) =>
      h.rel.endsWith("NoteRefinement.vue")
    )
    expect(noteRefinement?.count).toBe(2)

    const clientSetup = hits.find((h) => h.rel.endsWith("clientSetup.ts"))
    expect(clientSetup?.count).toBeGreaterThanOrEqual(1)
  })
})

describe("managedApi-only abort ownership", () => {
  it("keeps new AbortController only under managedApi/", () => {
    const outside = hitsFor(NEW_ABORT_CONTROLLER).filter(
      (h) => !h.rel.startsWith("managedApi/")
    )
    expect(outside).toEqual([])
  })

  it("keeps AbortError-name matching only under managedApi/", () => {
    const outside = hitsFor(ABORT_ERROR_NAME_MATCH).filter(
      (h) => !h.rel.startsWith("managedApi/")
    )
    expect(outside).toEqual([])
  })
})
