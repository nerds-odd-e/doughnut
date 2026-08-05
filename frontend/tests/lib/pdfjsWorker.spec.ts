import { describe, expect, it } from "vitest"
import { GlobalWorkerOptions } from "@/lib/pdfjsWorker"

describe("pdfjsWorker", () => {
  it("sets GlobalWorkerOptions.workerSrc on the same pdf.js module getDocument uses", () => {
    expect(GlobalWorkerOptions.workerSrc).toMatch(/pdfWorkerEntry/)
  })
})
