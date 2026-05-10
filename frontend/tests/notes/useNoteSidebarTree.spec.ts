import {
  createParentLocationDescriptionFrom,
  resolvedCreateParentFolderFrom,
  resolvedCreateParentFolderIdFrom,
  useNotebookRootCreateTarget,
} from "@/components/notes/useNoteSidebarTree"
import type {
  FolderPageClientView,
  NoteRealm,
} from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { computed, ref } from "vue"
import { describe, expect, it } from "vitest"

function folderPageClientStub(id: number, name: string): FolderPageClientView {
  const now = new Date().toISOString()
  return {
    notebook: makeMe.aNotebook.please(),
    folder: {
      id,
      name,
      createdAt: now,
      updatedAt: now,
    },
  }
}

describe("useNoteSidebarTree create context", () => {
  it("resolvedCreateParentFolderIdFrom prefers the sidebar-selected folder over the active note folder", () => {
    const realm = makeMe.aNoteRealm.inFolder(10, "From note").please()
    expect(
      resolvedCreateParentFolderIdFrom(
        folderPageClientStub(99, "Pinned"),
        realm,
        true
      )
    ).toBe(99)
  })

  it("resolvedCreateParentFolderFrom returns the sidebar-selected folder object when set", () => {
    const realm = makeMe.aNoteRealm.inFolder(10, "From note").please()
    const pinned = folderPageClientStub(99, "Pinned")
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
        folderPageClientStub(2, "My folder"),
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

  it("useNotebookRootCreateTarget matches the from helpers for the same refs", () => {
    const realm = makeMe.aNoteRealm.inFolder(8, "Lab").please()
    const activeFolder = ref<FolderPageClientView | null>(null)
    const activeNoteRealm = ref<NoteRealm | undefined>(realm)
    const noteContextResolved = ref(true)
    const {
      resolvedCreateParentFolder,
      resolvedCreateParentFolderRow,
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
    expect(resolvedCreateParentFolderRow.value).toEqual(
      realm.ancestorFolders!.at(-1)!
    )
    const pinned = folderPageClientStub(9, "Pinned")
    activeFolder.value = pinned
    expect(resolvedCreateParentFolderRow.value).toEqual(pinned.folder)
    expect(resolvedCreateParentFolderId.value).toBe(9)
    expect(createParentLocationDescription.value).toBe(
      'Adds to folder "Pinned".'
    )
  })

  it("useNotebookRootCreateTarget accepts computed realm ref", () => {
    const r = ref(makeMe.aNoteRealm.inFolder(2, "Docs").please())
    const activeRealm = computed(() => r.value)
    const { resolvedCreateParentFolderId } = useNotebookRootCreateTarget(
      ref(null),
      activeRealm,
      ref(true)
    )
    expect(resolvedCreateParentFolderId.value).toBe(2)
  })
})
