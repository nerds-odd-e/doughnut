import {
  NoteController,
  NotebookController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NoteNewButton from "@/components/notes/core/NoteNewButton.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { wrapWithNoteShortcutScope } from "@tests/helpers/noteShortcutScopeTestHelpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { screen } from "@testing-library/vue"

vi.mock("@/managedApi/AiReplyEventSource", () => ({
  default: class {
    onMessage = vi.fn(() => this)
    onError = vi.fn(() => this)
    start = vi.fn()
  },
}))

vi.mock("@/components/commons/Popups/usePopups", () => ({
  default: () => ({
    popups: {
      confirm: vi.fn().mockResolvedValue(false),
      alert: vi.fn(),
      options: vi.fn(),
      done: vi.fn(),
      register: vi.fn(),
      peek: vi.fn(),
    },
  }),
}))

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
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(SearchController, "semanticSearchWithin", [])
    mockSdkService(NoteController, "getRecentNotes", [])
    mockSdkService(NotebookController, "listNotebookFolderIndex", [])
    mockSdkService(NotebookController, "listNotebookFolderListing", {
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
    expect(screen.queryByTestId("note-new-form")).toBeNull()

    dispatchNoteNewShortcut()
    await flushPromises()

    expect(await screen.findByTestId("note-new-form")).toBeInTheDocument()
  })

  it.each([
    "input",
    "textarea",
  ])("ignores n while focus is in an %s", async (tagName) => {
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
  })

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

  it("ignores n when shortcut scope is inactive", async () => {
    const Harness = wrapWithNoteShortcutScope(
      NoteNewButton,
      {
        notebookId: realm.notebookRealm.notebook.id,
      },
      false
    )

    helper
      .component(Harness)
      .withCleanStorage()
      .withRouter()
      .mount({ attachTo: document.body })

    await flushPromises()
    dispatchNoteNewShortcut()
    await flushPromises()

    expect(screen.queryByTestId("note-new-form")).toBeNull()
  })
})
