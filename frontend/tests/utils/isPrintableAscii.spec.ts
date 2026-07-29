import { describe, expect, it } from "vitest"
import { isPrintableAscii } from "@/utils/isPrintableAscii"

describe("isPrintableAscii", () => {
  it("accepts a name with only printable ASCII characters", () => {
    expect(isPrintableAscii("Ben Notebook.zip")).toBe(true)
  })

  it("accepts sanitized special characters within ASCII", () => {
    expect(isPrintableAscii("Q&A What Why.zip")).toBe(true)
  })

  it("rejects a name containing non-ASCII characters", () => {
    expect(isPrintableAscii("筆記本.zip")).toBe(false)
  })

  it("rejects a name mangled by ISO-8859-1 decoding of UTF-8 bytes", () => {
    // Representative of "筆記本" reinterpreted byte-by-byte as ISO-8859-1: each
    // resulting character lands above the printable-ASCII range (0x20-0x7E).
    const mangled = String.fromCharCode(0xe7, 0xad, 0x86, 0xe8, 0xa8, 0x98)
    expect(isPrintableAscii(mangled)).toBe(false)
  })

  it("rejects an empty string", () => {
    expect(isPrintableAscii("")).toBe(false)
  })
})
