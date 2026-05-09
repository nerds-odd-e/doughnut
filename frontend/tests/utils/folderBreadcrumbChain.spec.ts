import { folderBreadcrumbChainFromFlatIndex } from "@/utils/folderBreadcrumbChain"
import { describe, expect, it } from "vitest"

describe("folderBreadcrumbChainFromFlatIndex", () => {
  it("orders root to leaf using parent pointers", () => {
    const leaf = {
      id: 3,
      name: "Leaf",
      createdAt: "2021-01-01T00:00:00Z",
      updatedAt: "2021-01-01T00:00:00Z",
    }
    const rows = [
      { id: 1, name: "Root" },
      { id: 2, name: "Mid", parentFolderId: 1 },
      { id: 3, name: "Leaf", parentFolderId: 2 },
    ]
    const chain = folderBreadcrumbChainFromFlatIndex(leaf, rows)
    expect(chain.map((f) => f.id)).toEqual([1, 2, 3])
    expect(chain[2]).toBe(leaf)
  })
})
