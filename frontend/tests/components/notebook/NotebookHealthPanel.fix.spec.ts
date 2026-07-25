import { NotebookHealthController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookHealthPanel from "@/components/notebook/NotebookHealthPanel.vue"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import {
  createHealthPanelSpies,
  emptyReportFixture,
  fixButton,
  mountPanel,
  notebookId,
  removeEmptyFoldersCheckbox,
  reportFixture,
} from "./notebookHealthPanelTestSupport"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { defineComponent } from "vue"

describe("NotebookHealthPanel Fix", () => {
  let lintSpy: ReturnType<typeof mockSdkService>
  let fixSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    ;({ lintSpy, fixSpy } = createHealthPanelSpies())
  })

  afterEach(() => {
    teardownGlobalClientForTesting()
  })

  const loadingModal = () => document.querySelector(".loading-modal-mask")

  function mountPanelWithGlobalModal() {
    const Host = defineComponent({
      components: { NotebookHealthPanel, GlobalApiLoadingModal },
      setup: () => ({ notebookId }),
      template: `
        <NotebookHealthPanel :notebook-id="notebookId" />
        <GlobalApiLoadingModal />
      `,
    })
    return helper.component(Host).mount({ attachTo: document.body })
  }

  async function runLintWithCheckbox(wrapper: VueWrapper, checked: boolean) {
    await removeEmptyFoldersCheckbox(wrapper).setValue(checked)
    await flushPromises()
    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await flushPromises()
  }

  it("keeps lint path-only when Remove empty folders is checked; Fix is separate gated control", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await runLintWithCheckbox(wrapper, true)

    expect(lintSpy).toHaveBeenCalledOnce()
    expect(lintSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: notebookId },
      })
    )
    const callOptions = lintSpy.mock.calls[0]?.[0] as Record<string, unknown>
    expect(callOptions).not.toHaveProperty("body")
    expect(fixSpy).not.toHaveBeenCalled()

    expect(wrapper.find('[data-testid="notebook-health-fix"]').exists()).toBe(
      true
    )
    expect(wrapper.find('[data-testid="notebook-health-apply"]').exists()).toBe(
      false
    )
    expect(wrapper.text()).not.toMatch(/\bApply\b/)
  })

  it("shows disabled Fix with fallback label before report and when checkbox is unchecked", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    const idleFix = fixButton(wrapper)
    expect((idleFix.element as HTMLButtonElement).disabled).toBe(true)
    expect(idleFix.text()).toBe("Remove empty folders")

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await flushPromises()

    const uncheckedFix = fixButton(wrapper)
    expect((uncheckedFix.element as HTMLButtonElement).disabled).toBe(true)
    expect(uncheckedFix.text()).toBe("Remove 1 empty folders")
  })

  it("disables Fix when checkbox is checked but empty_folders has no items", async () => {
    lintSpy = mockSdkService(
      NotebookHealthController,
      "lint",
      emptyReportFixture
    )
    const wrapper = mountPanel()
    await flushPromises()

    await runLintWithCheckbox(wrapper, true)

    const fix = fixButton(wrapper)
    expect((fix.element as HTMLButtonElement).disabled).toBe(true)
    expect(fix.text()).toBe("Remove empty folders")
  })

  it("enables Fix with count label when checkbox is checked and empty_folders has items", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await runLintWithCheckbox(wrapper, true)

    const fix = fixButton(wrapper)
    expect((fix.element as HTMLButtonElement).disabled).toBe(false)
    expect(fix.text()).toBe("Remove 1 empty folders")
    expect(fix.classes()).toContain("daisy-btn-secondary")
    expect(fix.classes()).not.toContain("daisy-btn-primary")
  })

  it("shows blocking loading overlay for Fix through post-fix re-lint", async () => {
    let resolveFix!: () => void
    const fixPending = new Promise<void>((resolve) => {
      resolveFix = resolve
    })
    let resolveRelint!: () => void
    const relintPending = new Promise<typeof emptyReportFixture>((resolve) => {
      resolveRelint = () => resolve(emptyReportFixture)
    })

    lintSpy = mockSdkService(NotebookHealthController, "lint", reportFixture)
    lintSpy.mockResolvedValueOnce(wrapSdkResponse(reportFixture))
    lintSpy.mockImplementationOnce(() =>
      relintPending.then((data) => wrapSdkResponse(data))
    )
    fixSpy = mockSdkServiceWithImplementation(
      NotebookHealthController,
      "fix",
      async () => {
        await fixPending
      }
    )

    const wrapper = mountPanelWithGlobalModal()
    await flushPromises()
    await runLintWithCheckbox(wrapper, true)
    expect(loadingModal()).toBeNull()

    await fixButton(wrapper).trigger("click")
    await wrapper.vm.$nextTick()

    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toMatch(/empty folders/i)

    resolveFix()
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(loadingModal()).toBeTruthy()
    expect(fixSpy).toHaveBeenCalledOnce()
    expect(lintSpy).toHaveBeenCalledTimes(2)

    resolveRelint()
    await flushPromises()
    expect(loadingModal()).toBeNull()
  })

  it("calls fix then re-lints and replaces report on success", async () => {
    const postFixReport = emptyReportFixture
    lintSpy = mockSdkService(NotebookHealthController, "lint", reportFixture)
    lintSpy.mockResolvedValueOnce(wrapSdkResponse(reportFixture))
    lintSpy.mockResolvedValueOnce(wrapSdkResponse(postFixReport))

    const wrapper = mountPanel()
    await flushPromises()

    await runLintWithCheckbox(wrapper, true)
    expect(wrapper.text()).toContain("Empty Shell")

    await fixButton(wrapper).trigger("click")
    await flushPromises()

    expect(fixSpy).toHaveBeenCalledOnce()
    expect(fixSpy).toHaveBeenCalledWith({
      path: { notebook: notebookId },
      body: { removeEmptyFolders: true },
    })
    const fixCall = fixSpy.mock.calls[0]?.[0] as
      | { body: Record<string, unknown> }
      | undefined
    expect(fixCall).toBeDefined()
    expect(Object.keys(fixCall!.body)).toEqual(["removeEmptyFolders"])

    expect(lintSpy).toHaveBeenCalledTimes(2)
    expect(fixSpy.mock.invocationCallOrder[0]).toBeLessThan(
      lintSpy.mock.invocationCallOrder[1]!
    )

    expect(wrapper.text()).not.toContain("Empty Shell")
    expect(wrapper.text()).toContain("Readme Only Shell")
  })

  it("keeps prior report and does not re-lint when fix fails", async () => {
    fixSpy.mockResolvedValue(wrapSdkError("fix failed"))

    const wrapper = mountPanel()
    await flushPromises()

    await runLintWithCheckbox(wrapper, true)
    expect(wrapper.text()).toContain("Empty Shell")

    await fixButton(wrapper).trigger("click")
    await flushPromises()

    expect(fixSpy).toHaveBeenCalledOnce()
    expect(lintSpy).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain("Empty Shell")
  })
})
