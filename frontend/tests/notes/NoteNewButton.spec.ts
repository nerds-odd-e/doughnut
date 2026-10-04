import {
  NoteController,
  NotebookFolderController,
  SearchController,
} from "@generated/donut-backend-api/sdk.gen"
import NoteNewButton from "@/components/notes/core/NoteNewButton.vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { screen } from "@testing-library/vue"

function dispatchNoteNewShortcut() {
  document.dispatchEvent(
    new KeyboardEvent("keydown", {
      key: "n",
      code: "KeyN",
      bubbles: true,
      cancelable: true,
    })
  )
}

describe("NoteNewButton keyboard shortcut", () => {
  const realm = makeMe.aNoteRealm.title("anchor note").please()

  beforeEach(() => {
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
    mockSdkService(NoteController, "getRecentNotes", [])
    mockSdkService(NotebookFolderController, "listNotebookFolderIndex", [])
    mockSdkService(NotebookFolderController, "listNotebookFolderListing", {
      folders: [],
    })
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("opens the new-note dialog when n is pressed and the button is mounted", async () => {
    helper
      .component(NoteNewButton)
      .withCleanStorage()
      .withRouter()
      .withProps({
        notebookId: realm.notebookRealm.notebook.id,
        titleSearchAnchorNote: realm.note,
        ancestorFolders: realm.ancestorFolders ?? [],
      })
      .mount({ attachTo: document.body })

    await flushPromises()
    expect(document.querySelector('[data-testid="note-new-form"]')).toBeNull()

    dispatchNoteNewShortcut()
    await flushPromises()

    expect(
      document.querySelector('[data-testid="note-new-form"]')
    ).not.toBeNull()
  })

  it.each(["input", "textarea"])(
    "ignores n while focus is in an %s",
    async (tagName) => {
      const field = document.createElement(tagName)
      document.body.append(field)

      helper
        .component(NoteNewButton)
        .withCleanStorage()
        .withRouter()
        .withProps({
          notebookId: realm.notebookRealm.notebook.id,
        })
        .mount({ attachTo: document.body })

      await flushPromises()
      field.focus()

      dispatchNoteNewShortcut()
      await flushPromises()

      expect(screen.queryByTestId("note-new-form")).toBeNull()
    }
  )

  it("advertises the n shortcut in the new note button title", async () => {
    helper
      .component(NoteNewButton)
      .withCleanStorage()
      .withRouter()
      .withProps({
        notebookId: realm.notebookRealm.notebook.id,
      })
      .mount({ attachTo: document.body })

    await flushPromises()
    expect(
      document.querySelector('button[title="New note (n)"]')
    ).not.toBeNull()
  })
})
