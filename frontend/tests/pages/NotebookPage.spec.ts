import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import { wrapSdkResponse } from "@tests/helpers"
import { beforeEach, describe, it, expect, vi } from "vitest"
import {
  clickNotebookIndexSave,
  hideSidebarButtonEl,
  mountNotebookPageReady,
  notebookIndexEditorEl,
  notebookIndexSaveEl,
  notebookPageRouter,
  setNotebookIndexDraft,
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
    { label: "no", indexContent: undefined as string | undefined },
    {
      label: "existing",
      indexContent: "# Existing notebook index",
    },
  ])(
    "shows index editor when notebook has $label indexContent",
    async ({ indexContent }) => {
      const notebook = makeMe.aNotebook.please()
      const { wrapper } = await mountNotebookPageReady(notebook, {
        indexContent,
      })

      expect(notebookIndexEditorEl(wrapper).exists()).toBe(true)
      expect(notebookIndexSaveEl(wrapper).exists()).toBe(true)
      wrapper.unmount()
    }
  )

  it("saves notebook index content directly to container on save", async () => {
    const notebook = makeMe.aNotebook.please()
    const saveSpy = vi
      .spyOn(NotebookController, "updateNotebookIndexContent")
      .mockResolvedValue(
        wrapSdkResponse({
          notebook,
          hasAttachedBook: false,
          readonly: false,
          indexContent: "New notebook index",
        })
      )

    const { wrapper } = await mountNotebookPageReady(notebook)
    await setNotebookIndexDraft(wrapper, "New notebook index")
    await clickNotebookIndexSave(wrapper)

    expect(saveSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: notebook.id },
        body: expect.objectContaining({
          content: expect.stringContaining("New notebook index"),
        }),
      })
    )
    saveSpy.mockRestore()
    wrapper.unmount()
  })
})
