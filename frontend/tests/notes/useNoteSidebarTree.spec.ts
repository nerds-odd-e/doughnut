import {
  createParentLocationDescriptionFrom,
  folderLabelForRealmFolderId,
  resolvedCreateParentFolderIdFrom,
} from "@/components/notes/useNoteSidebarTree"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"

describe("useNoteSidebarTree create context", () => {
  const realmInFolder = (folderId: number, folderName: string): NoteRealm => {
    const r = makeMe.aNoteRealm.title("child").please()
    return {
      ...r,
      ancestorFolders: [{ id: String(folderId), name: folderName }],
      note: {
        ...r.note,
        noteTopology: { ...r.note.noteTopology, folderId },
      },
    } as NoteRealm
  }

  it("resolvedCreateParentFolderIdFrom prefers the user-selected folder over the active note folder", () => {
    const realm = realmInFolder(10, "From note")
    expect(
      resolvedCreateParentFolderIdFrom({ id: 99, name: "Pinned" }, realm, true)
    ).toBe(99)
  })

  it("resolvedCreateParentFolderIdFrom uses the active note folder when no user-selected folder", () => {
    const realm = realmInFolder(42, "Science")
    expect(resolvedCreateParentFolderIdFrom(null, realm, true)).toBe(42)
  })

  it("resolvedCreateParentFolderIdFrom is null when topology is not resolved", () => {
    const realm = realmInFolder(7, "X")
    expect(resolvedCreateParentFolderIdFrom(null, realm, false)).toBe(null)
  })

  it("resolvedCreateParentFolderIdFrom is null for a note at the notebook root", () => {
    const realm = makeMe.aNoteRealm.title("root note").please()
    expect(resolvedCreateParentFolderIdFrom(null, realm, true)).toBe(null)
  })

  it("createParentLocationDescriptionFrom describes the user-selected folder by name", () => {
    const realm = realmInFolder(1, "ignored when user folder active")
    expect(
      createParentLocationDescriptionFrom(
        { id: 2, name: "My folder" },
        realm,
        true
      )
    ).toBe('Adds to folder "My folder".')
  })

  it("createParentLocationDescriptionFrom uses the realm trail when user folder name is empty", () => {
    const realm = realmInFolder(3, "Trail name")
    expect(
      createParentLocationDescriptionFrom({ id: 3, name: "" }, realm, true)
    ).toBe('Adds to folder "Trail name".')
  })

  it("createParentLocationDescriptionFrom uses ancestor folder label for the active note folder", () => {
    const realm = realmInFolder(5, "History")
    expect(createParentLocationDescriptionFrom(null, realm, true)).toBe(
      'Adds to folder "History".'
    )
  })

  it("createParentLocationDescriptionFrom uses Folder #id when ancestor trail has no name match", () => {
    const r = makeMe.aNoteRealm.please()
    const realm = {
      ...r,
      ancestorFolders: [],
      note: {
        ...r.note,
        noteTopology: { ...r.note.noteTopology, folderId: 404 },
      },
    } as NoteRealm
    expect(createParentLocationDescriptionFrom(null, realm, true)).toBe(
      'Adds to folder "Folder #404".'
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

  it("folderLabelForRealmFolderId falls back to Folder #id", () => {
    expect(folderLabelForRealmFolderId(undefined, 12)).toBe("Folder #12")
  })
})
