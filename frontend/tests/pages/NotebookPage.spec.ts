import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import { wrapSdkResponse } from "@tests/helpers"
import { beforeEach, describe, it, expect, vi } from "vitest"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import {
  hideSidebarButtonEl,
  mountNotebookPageReady,
  notebookReadmeEditorEl,
  notebookPageRouter,
  setNotebookReadmeDraft,
} from "./notebookPageTestSupport"

describe("NotebookPage.spec", () => {
  beforeEach(async () => {
    await notebookPageRouter.push("/")
  })

  it("shows New note in main column when sidebar is hidden", async () => {
    const notebook = makeMe.aNotebook.please()
    const { wrapper } = await mountNotebookPageReady(notebook)

    const hideSidebar = hideSidebarButtonEl()
    if (hideSidebar != null) {
      expect(
        wrapper.find('[data-testid="note-main-creation-toolbar"]').exists()
      ).toBe(false)
      await hideSidebar.click()
      await wrapper.vm.$nextTick()
    }

    expect(
      wrapper.find('[data-testid="note-main-creation-toolbar"]').exists()
    ).toBe(true)
    expect(
      wrapper.find('[data-testid="note-creation-new-button"]').exists()
    ).toBe(true)
    wrapper.unmount()
  })

  it.each([
    { label: "no", readmeContent: undefined as string | undefined },
    {
      label: "existing",
      readmeContent: "# Existing notebook readme",
    },
  ])(
    "shows readme editor when notebook has $label readmeContent",
    async ({ readmeContent }) => {
      const notebook = makeMe.aNotebook.please()
      const { wrapper } = await mountNotebookPageReady(notebook, {
        readmeContent,
      })

      expect(notebookReadmeEditorEl(wrapper).exists()).toBe(true)
      expect(
        wrapper.find('[data-testid="notebook-readme-save"]').exists()
      ).toBe(false)
      wrapper.unmount()
    }
  )

  it("auto-saves notebook readme content after debounce", async () => {
    vi.useFakeTimers()
    const notebook = makeMe.aNotebook.please()
    const saveSpy = vi
      .spyOn(NotebookController, "updateNotebookReadmeContent")
      .mockResolvedValue(
        wrapSdkResponse({
          notebook,
          hasAttachedBook: false,
          readonly: false,
          readmeContent: "New notebook readme",
        })
      )

    const { wrapper } = await mountNotebookPageReady(notebook)
    await setNotebookReadmeDraft(wrapper, "New notebook readme")
    await advanceNoteContentSaveDebounce()

    expect(saveSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: notebook.id },
        body: expect.objectContaining({
          content: expect.stringContaining("New notebook readme"),
        }),
      })
    )
    saveSpy.mockRestore()
    wrapper.unmount()
    vi.useRealTimers()
  })
})
