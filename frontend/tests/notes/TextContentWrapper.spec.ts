import { TextContentController } from "@generated/doughnut-backend-api/sdk.gen"
import TextContentWrapper from "@/components/notes/core/TextContentWrapper.vue"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi, afterEach } from "vitest"
import { h, nextTick } from "vue"

const waitForAnimationFrames = () =>
  new Promise<void>((resolve) => {
    requestAnimationFrame(() => requestAnimationFrame(() => resolve()))
  })

describe("TextContentWrapper referenced title rename", () => {
  afterEach(() => {
    document.body.innerHTML = ""
    vi.restoreAllMocks()
  })

  const titleSlot = (slotProps: {
    value: string
    update: (noteId: number, v: string) => void
    blur: () => void
  }) =>
    h("input", {
      "data-testid": "title-slot-input",
      value: slotProps.value,
      onInput: (e: Event) =>
        slotProps.update(1, (e.target as HTMLInputElement).value),
      onBlur: slotProps.blur,
    })

  const mountReferencedTitle = () =>
    helper
      .component(TextContentWrapper)
      .withCleanStorage()
      .withProps({
        field: "edit title",
        value: "Original",
        titleRenameNeedsExplicitReferenceChoice: true,
        titleEditNoteId: 42,
      })
      .mount({
        slots: { default: titleSlot },
        attachTo: document.body,
      })

  it("discards dirty title and hides save actions when focus leaves the wrapper", async () => {
    const wrapper = mountReferencedTitle()
    await flushPromises()

    const input = document.querySelector(
      "[data-testid=title-slot-input]"
    ) as HTMLInputElement
    input.focus()
    input.value = "Edited"
    input.dispatchEvent(new Event("input", { bubbles: true }))
    await nextTick()

    expect(
      document.querySelector("[data-testid=referenced-title-save-panel]")
    ).toBeTruthy()

    input.blur()
    await flushPromises()
    await waitForAnimationFrames()
    await nextTick()

    expect(input.value).toBe("Original")
    expect(
      document.querySelector("[data-testid=referenced-title-save-panel]")
    ).toBeNull()

    wrapper.unmount()
  })

  it("keeps the draft when choosing a save option (click does not discard before save)", async () => {
    const updateSpy = mockSdkService(
      TextContentController,
      "updateNoteTitle",
      makeMe.aNoteRealm.title("Edited").please()
    )

    const wrapper = mountReferencedTitle()
    await flushPromises()

    const input = document.querySelector(
      "[data-testid=title-slot-input]"
    ) as HTMLInputElement
    input.focus()
    input.value = "Edited"
    input.dispatchEvent(new Event("input", { bubbles: true }))
    await nextTick()

    const keepBtn = document.querySelector(
      "[data-testid=referenced-title-save-keep-visible-text]"
    ) as HTMLButtonElement

    keepBtn.click()
    await flushPromises()
    await waitForAnimationFrames()
    await nextTick()

    expect(input.value).toBe("Edited")
    expect(updateSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        body: expect.objectContaining({
          newTitle: "Edited",
          referenceHandling: "KEEP_VISIBLE_TEXT",
        }),
      })
    )

    wrapper.unmount()
  })

  it("does not discard when focusout has a misleading relatedTarget but focus remains inside the wrapper", async () => {
    const wrapper = mountReferencedTitle()
    await flushPromises()

    const input = document.querySelector(
      "[data-testid=title-slot-input]"
    ) as HTMLInputElement
    input.focus()
    input.value = "Edited"
    input.dispatchEvent(new Event("input", { bubbles: true }))
    await nextTick()

    const keepBtn = document.querySelector(
      "[data-testid=referenced-title-save-keep-visible-text]"
    ) as HTMLButtonElement
    keepBtn.focus()

    wrapper.element.dispatchEvent(
      new FocusEvent("focusout", {
        bubbles: false,
        relatedTarget: document.body,
      })
    )
    await flushPromises()
    await nextTick()

    expect(input.value).toBe("Edited")
    expect(
      document.querySelector("[data-testid=referenced-title-save-panel]")
    ).toBeTruthy()

    wrapper.unmount()
  })
})
