import {
  NotebookController,
  NotebookFolderController,
  NoteController,
  SearchController,
} from "@generated/donut-backend-api/sdk.gen"
import { noteShowLocation } from "@/routes/noteShowLocation"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, expect, it } from "vitest"
import { page } from "vitest/browser"
import {
  atViewportWidth,
  mountNotebookWorkspace,
} from "./notebookWorkspaceHeaderTestSupport"
import { deepNote, innerFolder } from "./notebookWorkspaceContextTestSupport"

let wrapper: VueWrapper
afterEach(() => {
  wrapper.unmount()
  document.body.innerHTML = ""
})
const cases = [320, 768].flatMap((width) =>
  ["root", "folder", "note"].flatMap((context) =>
    ["note", "folder"].map((create) => ({ width, context, create }))
  )
)
it.each(cases)(
  "creates $create in $context scope from contents at $width px",
  async ({ width, context, create }) => {
    await atViewportWidth(width)
    const realm = deepNote()
    const notebookId = realm.notebookRealm.notebook.id
    const folderRealm = {
      ...makeMe.aFolderRealm.folder(innerFolder.id, innerFolder.name).please(),
      notebookRealm: realm.notebookRealm,
      ancestorFolders: realm.ancestorFolders,
    }
    mockSdkService(NotebookFolderController, "getFolderPage", folderRealm)
    mockSdkService(NotebookFolderController, "listNotebookFolderIndex", [])
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
    mockSdkService(NoteController, "getRecentNotes", [])
    const location =
      context === "note"
        ? noteShowLocation(realm.id)
        : context === "root"
          ? { name: "notebookPage", params: { notebookId: String(notebookId) } }
          : {
              name: "folderPage",
              params: {
                notebookId: String(notebookId),
                folderId: String(innerFolder.id),
              },
            }
    const mounted = await mountNotebookWorkspace([realm], location)
    wrapper = mounted.wrapper
    if (width < 768) {
      await page
        .elementLocator(document.querySelector('[aria-label="Show sidebar"]')!)
        .click()
      await flushPromises()
    }
    const creation =
      create === "note"
        ? mockSdkService(NotebookController, "createNoteAtNotebookRoot", realm)
        : mockSdkService(
            NotebookFolderController,
            "createFolder",
            folderRealm.folder
          )
    await page
      .elementLocator(
        document.querySelector(
          `[data-note-sidebar-toolbar] [title="${create === "note" ? "New note (n)" : "New folder"}"]`
        )!
      )
      .click()
    await flushPromises()
    const select = document.querySelector<HTMLSelectElement>(
      '[data-testid="folder-move-parent-select"]'
    )!
    expect(select.value).toBe(
      context === "root" ? "__root__" : String(innerFolder.id)
    )
    const form = document.querySelector<HTMLFormElement>(
      create === "note"
        ? '[data-testid="note-new-form"]'
        : '[data-testid="folder-new-dialog"] form'
    )!
    const editor = form.querySelector<HTMLElement>(".seamless-editor")!
    editor.innerText = "New orchard item"
    editor.dispatchEvent(new Event("input", { bubbles: true }))
    form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }))
    await flushPromises()
    const parent = context === "root" ? undefined : innerFolder.id
    expect(creation).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: notebookId },
        body:
          create === "note"
            ? {
                newTitle: "New orchard item",
                ...(parent === undefined ? {} : { folderId: parent }),
              }
            : { name: "New orchard item", underFolderId: parent },
      })
    )
    expect(mounted.router.currentRoute.value.name).toBe(
      create === "note" ? "noteShow" : "folderPage"
    )
  }
)
