import {
  AiController,
  AssimilationController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import AssimilationPage from "@/pages/AssimilationPage.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockShowNoteAccessory,
  mockSdkService,
  wrapSdkResponse,
} from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import mockBrowserTimeZone from "@tests/helpers/mockBrowserTimeZone"

const mockedPush = vi.fn()

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      push: mockedPush,
    }),
  }
})

let renderer: RenderingHelper<typeof AssimilationPage>
let showNoteSpy: ReturnType<typeof mockSdkService>

afterEach(() => {
  document.body.innerHTML = ""
})

mockBrowserTimeZone("Europe/Amsterdam", beforeEach, afterEach)

beforeEach(() => {
  mockSdkService(AssimilationController, "assimilating", [])
  mockSdkService(NoteController, "getNoteInfo", {})
  showNoteSpy = mockSdkService(
    NoteController,
    "showNote",
    makeMe.aNoteRealm.please()
  )
  mockSdkService(AiController, "generateUnderstandingChecklist", {
    points: [],
  })
  mockShowNoteAccessory()
  renderer = helper.component(AssimilationPage).withCleanStorage().withProps({})
})

describe("repeat page", () => {
  it("shows completion message when nothing to recall", async () => {
    const assimilatingSpy = mockSdkService(
      AssimilationController,
      "assimilating",
      []
    )
    const wrapper = renderer.currentRoute({ name: "assimilate" }).mount()

    await flushPromises()

    expect(wrapper.text()).toContain(
      "Congratulations! You've achieved your daily assimilation goal!"
    )
    expect(assimilatingSpy).toBeCalledWith({
      query: { timezone: "Europe/Amsterdam" },
    })
  })

  describe("normal view", () => {
    const noteRealm = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService(AssimilationController, "assimilating", [
        noteRealm,
        noteRealm,
      ])
      showNoteSpy.mockResolvedValue(wrapSdkResponse(noteRealm))
    })

    it("normal view", async () => {
      const wrapper = renderer.currentRoute({ name: "assimilate" }).mount()
      await flushPromises()
      expect(wrapper.findAll(".paused")).toHaveLength(0)
      expect(wrapper.element.textContent).toContain("Assimilating: 0/2")
    })
  })
})
