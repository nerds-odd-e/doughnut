import {
  createParentLocationDescriptionFrom,
  resolvedCreateParentFolderFrom,
  resolvedCreateParentFolderIdFrom,
} from "@/components/notes/useNoteSidebarTree"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"

describe("useNoteSidebarTree create context", () => {
  it("resolvedCreateParentFolderIdFrom prefers the sidebar-selected folder over the active note folder", () => {
    const realm = makeMe.aNoteRealm.inFolder(10, "From note").please()
    expect(
      resolvedCreateParentFolderIdFrom(
        makeMe.aFolderRealm.folder(99, "Pinned").please(),
        realm,
        true
      )
    ).toBe(99)
  })

  it("resolvedCreateParentFolderFrom returns the sidebar-selected folder object when set", () => {
    const realm = makeMe.aNoteRealm.inFolder(10, "From note").please()
    const pinned = makeMe.aFolderRealm.folder(99, "Pinned").please()
    expect(resolvedCreateParentFolderFrom(pinned, realm, true)).toEqual(pinned)
  })

  it("resolvedCreateParentFolderFrom returns the realm leaf folder when none selected", () => {
    const realm = makeMe.aNoteRealm.inFolder(42, "Science").please()
    const leaf = realm.ancestorFolders!.at(-1)!
    expect(resolvedCreateParentFolderFrom(null, realm, true)).toBe(leaf)
  })

  it("resolvedCreateParentFolderIdFrom uses the active note folder when no sidebar-selected folder", () => {
    const realm = makeMe.aNoteRealm.inFolder(42, "Science").please()
    expect(resolvedCreateParentFolderIdFrom(null, realm, true)).toBe(42)
  })

  it("resolvedCreateParentFolderIdFrom is null when note context is not resolved", () => {
    const realm = makeMe.aNoteRealm.inFolder(7, "X").please()
    expect(resolvedCreateParentFolderIdFrom(null, realm, false)).toBe(null)
  })

  it("resolvedCreateParentFolderIdFrom is null for a note at the notebook root", () => {
    const realm = makeMe.aNoteRealm.please()
    expect(resolvedCreateParentFolderIdFrom(null, realm, true)).toBe(null)
  })

  it("createParentLocationDescriptionFrom describes the sidebar-selected folder by name", () => {
    const realm = makeMe.aNoteRealm
      .inFolder(1, "ignored when user folder active")
      .please()
    expect(
      createParentLocationDescriptionFrom(
        makeMe.aFolderRealm.folder(2, "My folder").please(),
        realm,
        true
      )
    ).toBe('Adds to folder "My folder".')
  })

  it("createParentLocationDescriptionFrom uses ancestor folder label for the active note folder", () => {
    const realm = makeMe.aNoteRealm.inFolder(5, "History").please()
    expect(createParentLocationDescriptionFrom(null, realm, true)).toBe(
      'Adds to folder "History".'
    )
  })

  it("createParentLocationDescriptionFrom describes notebook root when appropriate", () => {
    const realm = makeMe.aNoteRealm.please()
    expect(createParentLocationDescriptionFrom(null, realm, true)).toBe(
      "Adds to the notebook root."
    )
    expect(createParentLocationDescriptionFrom(null, realm, false)).toBe(
      "Adds to the notebook root."
    )
  })
})
