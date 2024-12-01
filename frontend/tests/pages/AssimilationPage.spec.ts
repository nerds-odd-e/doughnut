import AssimilationPage from "@/pages/AssimilationPage.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { useRouter } from "vue-router"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import mockBrowserTimeZone from "@tests/helpers/mockBrowserTimeZone"

const mockedPush = vi.fn()

vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: mockedPush,
  }),
}))

let renderer: RenderingHelper<typeof AssimilationPage>
const mockedInitialReviewCall = vi.fn()
const mockedNoteInfoCall = vi.fn()
const mockedGetNoteCall = vi.fn()

let teleportTarget: HTMLDivElement

beforeEach(() => {
  teleportTarget = document.createElement("div")
  teleportTarget.id = "head-status"
  document.body.appendChild(teleportTarget)
})
afterEach(() => {
  document.body.innerHTML = ""
})

mockBrowserTimeZone("Europe/Amsterdam", beforeEach, afterEach)

beforeEach(() => {
  helper.managedApi.assimilationController.assimilating =
    mockedInitialReviewCall
  helper.managedApi.restNoteController.getNoteInfo =
    mockedNoteInfoCall.mockResolvedValue({})
  helper.managedApi.restNoteController.show = mockedGetNoteCall
  renderer = helper.component(AssimilationPage).withStorageProps({})
})

describe("repeat page", () => {
  it("redirect to review page if nothing to review", async () => {
    mockedInitialReviewCall.mockResolvedValue([])
    renderer.currentRoute({ name: "assimilate" }).mount()
    await flushPromises()
    expect(useRouter().push).toHaveBeenCalledWith({ name: "recalls" })
    expect(mockedInitialReviewCall).toBeCalledWith("Europe/Amsterdam")
  })

  describe("normal view", () => {
    const noteRealm = makeMe.aNoteRealm.please()
    const memoryTracker = makeMe.aMemoryTracker.ofNote(noteRealm).please()
    const { note } = memoryTracker

    beforeEach(() => {
      mockedInitialReviewCall.mockResolvedValue([note, note])
      mockedGetNoteCall.mockResolvedValue(noteRealm)
    })

    it("normal view", async () => {
      const wrapper = renderer.currentRoute({ name: "assimilate" }).mount()
      await flushPromises()
      expect(wrapper.findAll(".paused")).toHaveLength(0)
      expect(teleportTarget.textContent).toContain("Initial Review: 0/2")
    })
  })
})
