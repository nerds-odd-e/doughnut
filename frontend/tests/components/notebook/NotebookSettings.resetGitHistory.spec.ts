import {
  NotebookBooksController,
  NotebookController,
} from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import NotebookSettings from "@/components/notebook/NotebookSettings.vue"

describe("NotebookSettings reset Git history", () => {
  const notebook = makeMe.aNotebook.id(42).please()

  beforeEach(() => {
    mockSdkService(NotebookBooksController, "getBook", undefined)
  })

  const mountSettings = () =>
    helper
      .component(NotebookSettings)
      .withProps({
        notebook,
        settingsBody: {
          description: "",
          skipMemoryTrackingEntirely: false,
        },
      })
      .withRouter()
      .mount()

  it("sends nothing when the owner cancels the Git history warning", async () => {
    const resetSpy = mockSdkService(
      NotebookController,
      "resetNotebookGitHistory",
      undefined
    )
    const wrapper = mountSettings()
    await flushPromises()

    await wrapper
      .get('[data-testid="notebook-settings-reset-git-history"]')
      .trigger("click")

    const popups = usePopups().popups.peek()
    expect(popups).toHaveLength(1)
    expect(popups[0]!.type).toBe("confirm")
    expect(popups[0]!.message).toContain("permanently discarded")
    expect(popups[0]!.message).toContain("cloned again")

    usePopups().popups.done(false)
    await flushPromises()

    expect(resetSpy).not.toHaveBeenCalled()
  })
})
