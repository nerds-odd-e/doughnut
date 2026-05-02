import { structuralSidebarTitlesFromRealm } from "@/components/notes/sidebarStructuralTitles"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, it, expect } from "vitest"

describe("structuralSidebarTitlesFromRealm", () => {
  it("returns empty set when realm is undefined", () => {
    expect([...structuralSidebarTitlesFromRealm(undefined)]).toEqual([])
  })

  it("includes active note title and ancestor folder names", () => {
    const realm = makeMe.aNoteRealm.title("Leaf").please()
    realm.ancestorFolders = [
      { id: "1", name: "Outer" },
      { id: "2", name: "Inner" },
    ]
    const titles = structuralSidebarTitlesFromRealm(realm)
    expect(titles.has("Leaf")).toBe(true)
    expect(titles.has("Outer")).toBe(true)
    expect(titles.has("Inner")).toBe(true)
    expect(titles.size).toBe(3)
  })

  it("does not walk parent note topology (only realm fields)", () => {
    const parent = makeMe.aNoteRealm.title("Parent").please()
    const child = makeMe.aNoteRealm.title("Child").under(parent).please()
    child.ancestorFolders = [{ id: "10", name: "Folder A" }]
    const titles = structuralSidebarTitlesFromRealm(child)
    expect(titles.has("Child")).toBe(true)
    expect(titles.has("Folder A")).toBe(true)
    expect(titles.has("Parent")).toBe(false)
  })
})
