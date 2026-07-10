import { TextContentController } from "@generated/doughnut-backend-api/sdk.gen"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import { mockSdkService } from "@tests/helpers"
import {
  contentSlotTextarea,
  editReferencedTitle,
  flushReferencedTitleBlurDiscardCheck,
  mockUpdateNoteTitleSuccess,
  mountContentWrapper,
  mountReferencedTitleReady,
  referencedTitleEdited,
  referencedTitleOriginal,
  referencedTitleSaveKeepVisibleTextButton,
  referencedTitleSavePanel,
  setupTextContentWrapperTests,
  titleSlotInput,
  wrapper,
} from "@tests/notes/textContentWrapperTestSupport"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi, afterEach } from "vitest"

describe("TextContentWrapper referenced title rename", () => {
  setupTextContentWrapperTests()

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("discards dirty title and hides save actions when focus leaves the wrapper", async () => {
    await mountReferencedTitleReady()

    const input = await editReferencedTitle()
    expect(referencedTitleSavePanel()).toBeTruthy()

    input.blur()
    await flushPromises()
    await flushReferencedTitleBlurDiscardCheck()

    expect(input.value).toBe(referencedTitleOriginal)
    expect(referencedTitleSavePanel()).toBeNull()
  })

  it("keeps the draft when choosing a save option (click does not discard before save)", async () => {
    const updateSpy = mockUpdateNoteTitleSuccess()

    await mountReferencedTitleReady()
    const input = await editReferencedTitle()

    referencedTitleSaveKeepVisibleTextButton().click()
    await flushPromises()

    expect(input.value).toBe(referencedTitleEdited)
    expect(updateSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        body: expect.objectContaining({
          newTitle: referencedTitleEdited,
          referenceHandling: "KEEP_VISIBLE_TEXT",
        }),
      })
    )
  })

  it("does not discard when focusout has a misleading relatedTarget but focus remains inside the wrapper", async () => {
    await mountReferencedTitleReady()
    await editReferencedTitle()

    const keepBtn = referencedTitleSaveKeepVisibleTextButton()
    keepBtn.focus()

    wrapper.element.dispatchEvent(
      new FocusEvent("focusout", {
        bubbles: false,
        relatedTarget: document.body,
      })
    )
    await flushPromises()
    await flushReferencedTitleBlurDiscardCheck()

    expect(titleSlotInput().value).toBe(referencedTitleEdited)
    expect(referencedTitleSavePanel()).toBeTruthy()
  })
})

describe("TextContentWrapper beforeSaveContent", () => {
  setupTextContentWrapperTests()

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  it("blocks save when beforeSaveContent returns false", async () => {
    vi.useFakeTimers()
    const beforeSaveContent = vi.fn().mockResolvedValue(false)
    const updateSpy = mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )

    mountContentWrapper(
      {
        value: "Original content",
        beforeSaveContent,
      },
      { attachTo: document.body }
    )
    await flushPromises()

    const textarea = contentSlotTextarea()
    textarea.value = "Edited content"
    textarea.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()
    await advanceNoteContentSaveDebounce()

    expect(beforeSaveContent).toHaveBeenCalledWith(
      "Original content",
      "Edited content"
    )
    expect(updateSpy).not.toHaveBeenCalled()
  })
})
