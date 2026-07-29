import { describe, expect, it } from "vitest"
import { contentDispositionFileName } from "@/utils/contentDispositionFileName"

describe("contentDispositionFileName", () => {
  it("reads the quoted filename the backend sends", () => {
    expect(
      contentDispositionFileName('attachment; filename="Ben Notebook.zip"')
    ).toBe("Ben Notebook.zip")
  })

  it("reads an unquoted filename", () => {
    expect(contentDispositionFileName("attachment; filename=Ben.zip")).toBe(
      "Ben.zip"
    )
  })

  it("reads the quoted filename among extra parameters", () => {
    expect(
      contentDispositionFileName(
        'attachment; filename="Ben Notebook.zip"; size=1234'
      )
    ).toBe("Ben Notebook.zip")
  })

  it("is undefined for a missing header", () => {
    expect(contentDispositionFileName(null)).toBeUndefined()
    expect(contentDispositionFileName(undefined)).toBeUndefined()
  })

  it("is undefined when the header has no filename parameter", () => {
    expect(contentDispositionFileName("attachment")).toBeUndefined()
  })
})
