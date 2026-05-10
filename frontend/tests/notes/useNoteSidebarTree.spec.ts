import {
  createParentLocationDescriptionFrom,
  resolvedCreateParentFolderFrom,
  resolvedCreateParentFolderIdFrom,
  useNotebookRootCreateTarget,
} from "@/components/notes/useNoteSidebarTree"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { testFolderStub } from "@tests/helpers"
import { computed, ref } from "vue"
import { describe, expect, it } from "vitest"

describe("useNoteSidebarTree create context", () => {
  const realmInFolder = (folderId: number, folderName: string): NoteRealm => {
    const r = makeMe.aNoteRealm.title("child").please()
    return {
      ...r,
      ancestorFolders: [testFolderStub(folderId, folderName)],
      note: {
        ...r.note,
        noteTopology: { ...r.note.noteTopology, folderId },
      },
    } as NoteRealm
  }

  it("resolvedCreateParentFolderIdFrom prefers the sidebar-selected folder over the active note folder", () => {
    const realm = realmInFolder(10, "From note")
    expect(
      resolvedCreateParentFolderIdFrom({ id: 99, name: "Pinned" }, realm, true)
    ).toBe(99)
  })

  it("resolvedCreateParentFolderFrom returns the sidebar-selected folder object when set", () => {
    const realm = realmInFolder(10, "From note")
    expect(
      resolvedCreateParentFolderFrom({ id: 99, name: "Pinned" }, realm, true)
    ).toEqual({ id: 99, name: "Pinned" })
  })

  it("resolvedCreateParentFolderFrom returns the realm leaf folder when none selected", () => {
    const realm = realmInFolder(42, "Science")
    const leaf = realm.ancestorFolders!.at(-1)!
    expect(resolvedCreateParentFolderFrom(null, realm, true)).toBe(leaf)
  })

  it("resolvedCreateParentFolderIdFrom uses the active note folder when no sidebar-selected folder", () => {
    const realm = realmInFolder(42, "Science")
    expect(resolvedCreateParentFolderIdFrom(null, realm, true)).toBe(42)
  })

  it("resolvedCreateParentFolderIdFrom is null when note context is not resolved", () => {
    const realm = realmInFolder(7, "X")
    expect(resolvedCreateParentFolderIdFrom(null, realm, false)).toBe(null)
  })

  it("resolvedCreateParentFolderIdFrom is null for a note at the notebook root", () => {
    const realm = makeMe.aNoteRealm.title("root note").please()
    expect(resolvedCreateParentFolderIdFrom(null, realm, true)).toBe(null)
  })

  it("createParentLocationDescriptionFrom describes the sidebar-selected folder by name", () => {
    const realm = realmInFolder(1, "ignored when user folder active")
    expect(
      createParentLocationDescriptionFrom(
        { id: 2, name: "My folder" },
        realm,
        true
      )
    ).toBe('Adds to folder "My folder".')
  })

  it("createParentLocationDescriptionFrom uses ancestor folder label for the active note folder", () => {
    const realm = realmInFolder(5, "History")
    expect(createParentLocationDescriptionFrom(null, realm, true)).toBe(
      'Adds to folder "History".'
    )
  })

  it("createParentLocationDescriptionFrom ignores topology folderId when ancestorFolders is empty", () => {
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
      "Adds to the notebook root."
    )
    expect(resolvedCreateParentFolderIdFrom(null, realm, true)).toBe(null)
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

  it("useNotebookRootCreateTarget matches the from helpers for the same refs", () => {
    const realm = realmInFolder(8, "Lab")
    const activeFolder = ref<{ id: number; name: string } | null>(null)
    const activeNoteRealm = ref<NoteRealm | undefined>(realm)
    const noteContextResolved = ref(true)
    const {
      resolvedCreateParentFolder,
      resolvedCreateParentFolderId,
      createParentLocationDescription,
    } = useNotebookRootCreateTarget(
      activeFolder,
      activeNoteRealm,
      noteContextResolved
    )
    expect(resolvedCreateParentFolder.value).toEqual(
      resolvedCreateParentFolderFrom(
        activeFolder.value,
        activeNoteRealm.value,
        noteContextResolved.value
      )
    )
    expect(resolvedCreateParentFolderId.value).toBe(
      resolvedCreateParentFolderIdFrom(
        activeFolder.value,
        activeNoteRealm.value,
        noteContextResolved.value
      )
    )
    expect(createParentLocationDescription.value).toBe(
      createParentLocationDescriptionFrom(
        activeFolder.value,
        activeNoteRealm.value,
        noteContextResolved.value
      )
    )
    activeFolder.value = { id: 9, name: "Pinned" }
    expect(resolvedCreateParentFolderId.value).toBe(9)
    expect(createParentLocationDescription.value).toBe(
      'Adds to folder "Pinned".'
    )
  })

  it("useNotebookRootCreateTarget accepts computed realm ref", () => {
    const r = ref(realmInFolder(2, "Docs"))
    const activeRealm = computed(() => r.value)
    const { resolvedCreateParentFolderId } = useNotebookRootCreateTarget(
      ref(null),
      activeRealm,
      ref(true)
    )
    expect(resolvedCreateParentFolderId.value).toBe(2)
  })
})
