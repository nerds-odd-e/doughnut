import {
  folderPathLabel,
  folderRowsById,
  siblingFoldersForQuickPick,
} from "@/components/notes/folderSelectorUtils"
import { testFolderStub } from "@tests/helpers"
import { describe, expect, it } from "vitest"

describe("folderSelectorUtils", () => {
  it("folderPathLabel stops at a self-referential parent link", () => {
    const cyclic = { ...testFolderStub(1, "Alpha"), parentFolderId: 1 }
    const byId = folderRowsById([cyclic])
    expect(folderPathLabel(1, byId)).toBe("Alpha")
  })

  it("siblingFoldersForQuickPick stamps siblings with the parent and omits the parent row", () => {
    const alpha = testFolderStub(1, "Alpha")
    const gamma = testFolderStub(3, "Gamma")
    expect(siblingFoldersForQuickPick([alpha, gamma], 1)).toEqual([
      { ...gamma, parentFolderId: 1 },
    ])
  })
})
