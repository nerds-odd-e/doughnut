import { AiController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { nextTick } from "vue"
import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { showToastsOnPage, toastOnPage } from "@tests/helpers/toastTestSupport"
import {
  clickLoadingModalCancel,
  clickRetryRefinementLayout,
  createDeferredGate,
  loadingModalMask,
} from "./noteRefinementLayoutLoadingTestSupport"
import { clickRemoveRefinementLayout } from "./noteRefinementRemoveTestSupport"
import {
  note,
  refinementLayoutItems,
  renderer,
  selectFirstLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()
showToastsOnPage()

const OUTPUT_LIMIT = "Generation could not finish within its limit."
const UNUSABLE = "AI could not generate a refinement layout."

const problem = (message: string) =>
  wrapSdkError({
    message,
    errorType: "BINDING_ERROR",
    errors: {},
  })

const explanation = (wrapper: VueWrapper) =>
  wrapper.find('[data-test-id="refinement-layout-error"]')

const retry = (wrapper: VueWrapper) =>
  wrapper.find('[data-test-id="retry-refinement-layout"]')

describe("NoteRefinement layout generation failure", () => {
  it("shows the output limit beside retry, then the layout after a successful retry", async () => {
    const generateSpy = mockSdkService(
      AiController,
      "generateRefinementSuggestions",
      { items: [] }
    )
    generateSpy
      .mockResolvedValueOnce(problem(OUTPUT_LIMIT))
      .mockResolvedValueOnce(
        wrapSdkResponse({ items: refinementLayoutItems(["Cut a rope"]) })
      )
    const wrapper = renderer.withCleanStorage().withProps({ note }).mount()
    await flushPromises()

    const panel = wrapper.find('[data-test-id="refinement-layout-empty"]')
    expect(panel.find('[data-test-id="refinement-layout-error"]').text()).toBe(
      OUTPUT_LIMIT
    )
    expect(
      panel.find('[data-test-id="retry-refinement-layout"]').text()
    ).toContain("Ask AI to retry")

    await clickRetryRefinementLayout(wrapper)
    await flushPromises()

    expect(generateSpy).toHaveBeenCalledTimes(2)
    expect(explanation(wrapper).exists()).toBe(false)
    expect(
      wrapper.find('[data-test-id="refinement-layout-item-p1"]').text()
    ).toContain("Cut a rope")
    expect(
      wrapper.find('[data-test-id="refinement-layout-checkbox-p1"]').exists()
    ).toBe(true)
  })

  it("shows a generic explanation beside retry when the layout is unusable", async () => {
    mockSdkService(
      AiController,
      "generateRefinementSuggestions",
      undefined
    ).mockResolvedValue(problem(UNUSABLE))
    const wrapper = renderer.withCleanStorage().withProps({ note }).mount()
    await flushPromises()

    const panel = wrapper.find('[data-test-id="refinement-layout-empty"]')
    expect(explanation(wrapper).text()).toBe(UNUSABLE)
    expect(retry(wrapper).text()).toContain("Ask AI to retry")
    expect(panel.element.contains(explanation(wrapper).element)).toBe(true)
    expect(panel.element.contains(retry(wrapper).element)).toBe(true)
  })

  it("keeps a completed empty layout free of a failure explanation", async () => {
    mockSdkService(AiController, "generateRefinementSuggestions", { items: [] })
    const wrapper = renderer.withCleanStorage().withProps({ note }).mount()
    await flushPromises()

    expect(explanation(wrapper).exists()).toBe(false)
    expect(retry(wrapper).text()).toContain("Ask AI to retry")
  })

  it("cancels a retry quietly and ignores the late layout", async () => {
    const { gate, resolve } = createDeferredGate()
    const generateSpy = mockSdkService(
      AiController,
      "generateRefinementSuggestions",
      { items: [] }
    )
    generateSpy
      .mockResolvedValueOnce(problem(OUTPUT_LIMIT))
      .mockImplementationOnce(async () => {
        await gate
        return wrapSdkResponse({
          items: refinementLayoutItems(["Late retry should not appear"]),
        })
      })
    const wrapper = renderer.withCleanStorage().withProps({ note }).mount()
    await flushPromises()

    await clickRetryRefinementLayout(wrapper)
    await nextTick()
    expect(loadingModalMask()).toBeTruthy()

    clickLoadingModalCancel()
    await flushPromises()

    expect(loadingModalMask()).toBeNull()
    expect(toastOnPage()?.textContent).toContain(OUTPUT_LIMIT)
    expect(
      document.querySelectorAll(".Vue-Toastification__toast")
    ).toHaveLength(1)
    expect(explanation(wrapper).text()).toBe(OUTPUT_LIMIT)
    expect(retry(wrapper).exists()).toBe(true)
    expect(wrapper.emitted("contentUpdated")).toBeUndefined()

    resolve()
    await flushPromises()
    expect(document.body.textContent).not.toContain(
      "Late retry should not appear"
    )
    expect(explanation(wrapper).text()).toBe(OUTPUT_LIMIT)
  })

  it("shows the reload failure after the learner chooses removal", async () => {
    const noteWithContent = makeMe.aNote.content("Original content").please()
    const generateSpy = mockSdkService(
      AiController,
      "generateRefinementSuggestions",
      { items: [] }
    )
    generateSpy
      .mockResolvedValueOnce(
        wrapSdkResponse({ items: refinementLayoutItems(["Point 1"]) })
      )
      .mockResolvedValueOnce(problem(UNUSABLE))
    const wrapper = renderer
      .withCleanStorage()
      .withProps({ note: noteWithContent })
      .mount()
    await flushPromises()

    expect(wrapper.emitted("contentUpdated")).toBeUndefined()
    await selectFirstLayoutItem(wrapper)
    await clickRemoveRefinementLayout(wrapper)

    expect(generateSpy).toHaveBeenCalledTimes(2)
    expect(wrapper.emitted("contentUpdated")).toEqual([["Updated content"]])
    expect(explanation(wrapper).text()).toBe(UNUSABLE)
    expect(retry(wrapper).text()).toContain("Ask AI to retry")
  })
})
