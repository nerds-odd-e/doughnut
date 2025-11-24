import AssimilationPage from "@/pages/AssimilationPage.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockShowNoteAccessory } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import mockBrowserTimeZone from "@tests/helpers/mockBrowserTimeZone"
import { AssimilationController, NoteController } from "@generated/backend/sdk.gen"

const mockedPush = vi.fn()

vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: mockedPush,
  }),
}))

let renderer: RenderingHelper<typeof AssimilationPage>
const mockedGetNoteCall = vi.fn()

afterEach(() => {
  document.body.innerHTML = ""
})

mockBrowserTimeZone("Europe/Amsterdam", beforeEach, afterEach)

beforeEach(() => {
  vi.spyOn(AssimilationController, "assimilating").mockResolvedValue({
    data: [],
    error: undefined,
    request: {} as Request,
    response: {} as Response,
  })
  vi.spyOn(NoteController, "getNoteInfo").mockResolvedValue({
    data: {} as never,
    error: undefined,
    request: {} as Request,
    response: {} as Response,
  })
  vi.spyOn(NoteController, "showNote").mockImplementation(async (options) => {
    const result = await mockedGetNoteCall(options)
    return {
      data: result,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    }
  })
  mockShowNoteAccessory()
  renderer = helper.component(AssimilationPage).withStorageProps({})
})

describe("repeat page", () => {
  it("shows completion message when nothing to review", async () => {
    vi.spyOn(AssimilationController, "assimilating").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    const wrapper = renderer.currentRoute({ name: "assimilate" }).mount()
    await flushPromises()
    expect(wrapper.text()).toContain(
      "Congratulations! You've achieved your daily assimilation goal!"
    )
    expect(vi.mocked(AssimilationController.assimilating)).toBeCalledWith({
      query: { timezone: "Europe/Amsterdam" },
    })
  })

  describe("normal view", () => {
    const noteRealm = makeMe.aNoteRealm.please()
    const memoryTracker = makeMe.aMemoryTracker.ofNote(noteRealm).please()
    const { note } = memoryTracker

    beforeEach(() => {
      vi.spyOn(AssimilationController, "assimilating").mockResolvedValue({
        data: [note, note],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      mockedGetNoteCall.mockResolvedValue(noteRealm)
    })

    it("normal view", async () => {
      const wrapper = renderer.currentRoute({ name: "assimilate" }).mount()
      await flushPromises()
      expect(wrapper.findAll(".paused")).toHaveLength(0)
      expect(wrapper.element.textContent).toContain("Assimilating: 0/2")
    })
  })
})
