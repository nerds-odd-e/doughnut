import { useRecallData } from "@/composables/useRecallData"
import makeMe from "doughnut-test-fixtures/makeMe"
import { wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import {
  createUseRecallDataMock,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/components/commons/Popups/usePopups")

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({ path: "/", fullPath: "/" }),
    useRouter: () => ({ currentRoute: { value: { name: "recall" } } }),
  }
})

describe("RecallPage load more buttons", () => {
  const ctx = useRecallPageSpecContext()

  beforeEach(() => {
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({ toRepeat: [] })
    )
  })

  it("should show loading indicator when load more button is clicked", async () => {
    const wrapper = await ctx.mountPage()
    expect(wrapper.text()).toContain(
      "You have finished all recalls for this half a day!"
    )
    expect(wrapper.find("button.daisy-btn-secondary").exists()).toBe(true)

    let resolveRecalling: (value: unknown) => void
    const pendingPromise = new Promise((resolve) => {
      resolveRecalling = resolve
    })
    // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions
    ctx.recallingSpy.mockReturnValueOnce(pendingPromise as any)

    await wrapper.find("button.daisy-btn-secondary").trigger("click")
    await wrapper.vm.$nextTick()

    expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(true)
    expect(wrapper.text()).toContain("Loading more items...")
    expect(wrapper.find("button.daisy-btn-secondary").exists()).toBe(false)

    resolveRecalling!(wrapSdkResponse(makeMe.aDueMemoryTrackersList.please()))
    await flushPromises()
    expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(false)
  })
})
