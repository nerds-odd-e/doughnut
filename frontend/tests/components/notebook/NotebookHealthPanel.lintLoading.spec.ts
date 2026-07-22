import { NotebookHealthController } from "@generated/doughnut-backend-api/sdk.gen"
import {
  createHealthPanelSpies,
  fixButton,
  holdPendingLint,
  mountPanel,
} from "./notebookHealthPanelTestSupport"
import {
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"

describe("NotebookHealthPanel lint loading", () => {
  let lintSpy: ReturnType<typeof mockSdkServiceWithImplementation>

  beforeEach(() => {
    ;({ lintSpy } = createHealthPanelSpies())
  })

  it("disables Run and shows body spinner while lint is in flight", async () => {
    const pending = holdPendingLint()
    lintSpy = mockSdkServiceWithImplementation(
      NotebookHealthController,
      "lint",
      () => pending.promise
    )

    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await wrapper.vm.$nextTick()

    const runButton = wrapper.get('[data-testid="notebook-health-run"]')
      .element as HTMLButtonElement
    expect(runButton.disabled).toBe(true)
    expect(
      wrapper.find('[data-testid="notebook-health-lint-spinner"]').exists()
    ).toBe(true)
    expect(wrapper.find('[data-testid="notebook-health-idle"]').exists()).toBe(
      false
    )
    expect(
      wrapper.find('[data-testid="notebook-health-findings"]').exists()
    ).toBe(false)
    expect((fixButton(wrapper).element as HTMLButtonElement).disabled).toBe(
      true
    )

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    expect(lintSpy).toHaveBeenCalledOnce()

    pending.resolve()
    await flushPromises()

    expect(runButton.disabled).toBe(false)
    expect(
      wrapper.find('[data-testid="notebook-health-lint-spinner"]').exists()
    ).toBe(false)
    expect(
      wrapper.find('[data-testid="notebook-health-findings"]').exists()
    ).toBe(true)
  })

  it("hides stale findings behind spinner when re-running lint", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await flushPromises()
    expect(
      wrapper.find('[data-testid="notebook-health-findings"]').exists()
    ).toBe(true)

    const pending = holdPendingLint()
    lintSpy.mockImplementationOnce(() =>
      pending.promise.then((data) => wrapSdkResponse(data))
    )

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await wrapper.vm.$nextTick()

    expect(
      wrapper.find('[data-testid="notebook-health-lint-spinner"]').exists()
    ).toBe(true)
    expect(
      wrapper.find('[data-testid="notebook-health-findings"]').exists()
    ).toBe(false)

    pending.resolve()
    await flushPromises()

    expect(
      wrapper.find('[data-testid="notebook-health-lint-spinner"]').exists()
    ).toBe(false)
    expect(
      wrapper.find('[data-testid="notebook-health-findings"]').exists()
    ).toBe(true)
  })
})
