import Assimilation from "@/components/recall/Assimilation.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, {
  mockShowNoteAccessory,
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
  wrapSdkError,
} from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { computed, nextTick, ref } from "vue"
import usePopups from "@/components/commons/Popups/usePopups"
import type { PromotePointRequestDto } from "@generated/backend"

const PromotionType = {
  CHILD: "CHILD",
  SIBLING: "SIBLING",
} as const satisfies Record<string, PromotePointRequestDto["promotionType"]>

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useAssimilationCount")

let renderer: RenderingHelper<typeof Assimilation>
let assimilateSpy: ReturnType<typeof mockSdkService<"assimilate">>
let showNoteSpy: ReturnType<typeof mockSdkService<"showNote">>
const mockedIncrementAssimilatedCount = vi.fn()
const mockedTotalAssimilatedCount = ref(0)
const toRepeat = ref<
  Array<{ memoryTrackerId?: number; spelling?: boolean }> | undefined
>(undefined)

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
  // Clear any remaining popups
  const popups = usePopups()
  while (popups.popups.peek().length) {
    popups.popups.done(false)
  }
})

beforeEach(() => {
  assimilateSpy = mockSdkService("assimilate", [])
  showNoteSpy = mockSdkService("showNote", makeMe.aNoteRealm.please())
  mockSdkService("getNoteInfo", {
    note: makeMe.aNoteRealm.please(),
    createdAt: "",
    noteType: undefined,
  })
  mockSdkService("generateUnderstandingChecklist", {
    points: [],
  })
  mockSdkService("updateNoteType", undefined)

  vi.mocked(useRecallData).mockReturnValue({
    totalAssimilatedCount: mockedTotalAssimilatedCount,
    toRepeatCount: computed(() => toRepeat.value?.length ?? 0),
    toRepeat: ref(undefined),
    currentRecallWindowEndAt: ref(undefined),
    isRecallPaused: ref(false),
    shouldResumeRecall: ref(false),
    treadmillMode: ref(false),
    currentIndex: ref(0),
    diligentMode: ref(false),
    setToRepeat: vi.fn(),
    setCurrentRecallWindowEndAt: vi.fn(),
    setTotalAssimilatedCount: vi.fn(),
    setIsRecallPaused: vi.fn(),
    resumeRecall: vi.fn(),
    clearShouldResumeRecall: vi.fn(),
    setTreadmillMode: vi.fn(),
    setCurrentIndex: vi.fn(),
    setDiligentMode: vi.fn(),
  })

  vi.mocked(useAssimilationCount).mockReturnValue({
    incrementAssimilatedCount: mockedIncrementAssimilatedCount,
    dueCount: ref(0),
    setDueCount: vi.fn(),
    assimilatedCountOfTheDay: ref(0),
    setAssimilatedCountOfTheDay: vi.fn(),
    totalUnassimilatedCount: ref(0),
    setTotalUnassimilatedCount: vi.fn(),
  })

  mockShowNoteAccessory()
  renderer = helper.component(Assimilation)
})

describe("Assimilation component", () => {
  const noteRealm = makeMe.aNoteRealm.please()
  const memoryTracker = makeMe.aMemoryTracker.ofNote(noteRealm).please()
  const { note } = memoryTracker

  const mount = (overrides?: { note?: typeof note }) =>
    renderer
      .withCleanStorage()
      .withProps({ note: overrides?.note ?? note })
      .withRouter()
      .mount()

  const mountWithChecklist = (
    points: string[],
    opts?: { note?: typeof note }
  ) => {
    mockSdkService("generateUnderstandingChecklist", { points })
    return mount(opts)
  }

  const checklist = (wrapper: {
    find: (s: string) => { findAll: (s: string) => unknown[] }
  }) => wrapper.find('[data-test-id="understanding-checklist"]')

  const selectFirstCheckpoint = async (wrapper: ReturnType<typeof mount>) => {
    const checkboxes = checklist(wrapper).findAll('input[type="checkbox"]') as {
      setValue: (v: boolean) => Promise<void>
    }[]
    await checkboxes[0]?.setValue(true)
    await flushPromises()
  }

  beforeEach(() => {
    showNoteSpy.mockResolvedValue(wrapSdkResponse(noteRealm))
  })

  describe("normal assimilation", () => {
    it("emits initialReviewDone and increments counts correctly when assimilating normally", async () => {
      assimilateSpy.mockResolvedValue(
        wrapSdkResponse([
          { id: 1, removedFromTracking: false },
          { id: 2, removedFromTracking: true },
          { id: 3, removedFromTracking: false },
        ])
      )
      const wrapper = mount()

      await flushPromises()
      await wrapper.find('[data-test="keep-for-repetition"]').trigger("click")
      await flushPromises()

      expect(assimilateSpy).toHaveBeenCalledWith({
        body: { noteId: note.id, skipMemoryTracking: false },
      })
      expect(wrapper.emitted()).toHaveProperty("initialReviewDone")
      expect(mockedTotalAssimilatedCount.value).toBe(2)
      expect(mockedIncrementAssimilatedCount).toHaveBeenCalledWith(2)
    })
  })

  describe("note type selection via NoteInfoBar", () => {
    it("displays note type selection in NoteInfoBar", async () => {
      const wrapper = mount()
      await flushPromises()
      expect(
        wrapper.find('[data-test="note-type-selection-dialog"]').exists()
      ).toBe(true)
    })
  })

  describe("promote point to child note", () => {
    it("displays promote button for each understanding point", async () => {
      const wrapper = mountWithChecklist(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      const listItems = wrapper.findAll("li")
      expect(listItems).toHaveLength(3)
      listItems.forEach((li, index) => {
        expect(li.text()).toContain(["Point 1", "Point 2", "Point 3"][index])
        expect(li.find("button").exists()).toBe(true)
      })
    })

    it("calls promotePoint API when child button is clicked", async () => {
      const promotePointSpy = mockSdkService("promotePoint", {
        createdNote: makeMe.aNoteRealm.please(),
        updatedParentNote: makeMe.aNoteRealm.please(),
      })
      const wrapper = mountWithChecklist(["Test Point"])
      await flushPromises()

      await wrapper.find("li button").trigger("click")
      await flushPromises()

      expect(promotePointSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { point: "Test Point", promotionType: PromotionType.CHILD },
      })
    })

    it("removes point from checklist after successful creation", async () => {
      mockSdkService("promotePoint", {
        createdNote: makeMe.aNoteRealm.please(),
        updatedParentNote: makeMe.aNoteRealm.please(),
      })
      const wrapper = mountWithChecklist(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      await wrapper.findAll("li")[1]!.find("button").trigger("click")
      await flushPromises()

      expect(wrapper.findAll("li")).toHaveLength(2)
      expect(wrapper.text()).toContain("Point 1")
      expect(wrapper.text()).toContain("Point 3")
      expect(wrapper.text()).not.toContain("Point 2")
    })

    it("keeps point in checklist when API fails", async () => {
      mockSdkService("promotePoint", undefined).mockResolvedValue(
        wrapSdkError("API Error")
      )
      const wrapper = mountWithChecklist(["Test Point"])
      await flushPromises()

      await wrapper.find("li button").trigger("click")
      await flushPromises()

      expect(wrapper.findAll("li")).toHaveLength(1)
      expect(wrapper.text()).toContain("Test Point")
    })

    it("calls promotePoint with SIBLING when sibling button is clicked", async () => {
      const childNoteRealm = makeMe.aNoteRealm
        .under(makeMe.aNoteRealm.please())
        .please()
      const childNote = makeMe.aMemoryTracker
        .ofNote(childNoteRealm)
        .please().note
      const promotePointSpy = mockSdkService("promotePoint", {
        createdNote: makeMe.aNoteRealm.please(),
        updatedParentNote: makeMe.aNoteRealm.please(),
      })
      const wrapper = mountWithChecklist(["Test Point"], { note: childNote })
      await flushPromises()

      await wrapper.findAll("li button")[1]!.trigger("click")
      await flushPromises()

      expect(promotePointSpy).toHaveBeenCalledWith({
        path: { note: childNote.id },
        body: { point: "Test Point", promotionType: PromotionType.SIBLING },
      })
    })

    it("hides sibling button when note has no parent", async () => {
      const wrapper = mountWithChecklist(["Test Point"])
      await flushPromises()

      expect(
        wrapper.find('button[title="Promote to sibling note"]').exists()
      ).toBe(false)
      expect(
        wrapper.find('button[title="Promote to child note"]').exists()
      ).toBe(true)
    })
  })

  describe("understanding checklist deletion", () => {
    it("shows checkboxes for each understanding point", async () => {
      const wrapper = mountWithChecklist(["Point 1", "Point 2", "Point 3"])
      await flushPromises()
      expect(checklist(wrapper).findAll('input[type="checkbox"]')).toHaveLength(
        3
      )
    })

    it("disables delete button when no points are selected", async () => {
      const wrapper = mountWithChecklist(["Point 1", "Point 2"])
      await flushPromises()
      expect(
        (
          wrapper.find('[data-test-id="delete-understanding-points"]')
            .element as HTMLButtonElement
        ).disabled
      ).toBe(true)
    })

    it("enables delete button when a point is selected", async () => {
      const wrapper = mountWithChecklist(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      expect(
        (
          wrapper.find('[data-test-id="delete-understanding-points"]')
            .element as HTMLButtonElement
        ).disabled
      ).toBe(false)
    })

    it("shows confirmation dialog when delete button is clicked", async () => {
      const wrapper = mountWithChecklist(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups).toHaveLength(1)
      expect(popups[0]!.type).toBe("confirm")
      expect(popups[0]!.message).toContain("delete")
    })

    it("calls API and emits reloadNeeded when deletion is confirmed", async () => {
      const deletePointsSpy = mockSdkService("removePointFromNote", {})
      const wrapper = mountWithChecklist(["Point 1", "Point 2", "Point 3"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()

      expect(deletePointsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { points: ["Point 1"] },
      })
      expect(wrapper.emitted()).toHaveProperty("reloadNeeded")
    })

    it("does not call API when deletion is cancelled", async () => {
      const deletePointsSpy = mockSdkService("removePointFromNote", {})
      const wrapper = mountWithChecklist(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(false)
      await flushPromises()

      expect(deletePointsSpy).not.toHaveBeenCalled()
      expect(wrapper.emitted()).not.toHaveProperty("reloadNeeded")
    })
  })

  describe("LoadingModal for Promote Point", () => {
    it("shows LoadingModal while promoting point", async () => {
      let resolveApi: () => void
      const apiPromise = new Promise<void>((r) => {
        resolveApi = r
      })
      mockSdkServiceWithImplementation("promotePoint", async () => {
        await apiPromise
        return {
          createdNote: makeMe.aNoteRealm.please(),
          updatedParentNote: makeMe.aNoteRealm.please(),
        }
      })
      const wrapper = mountWithChecklist(["Test understanding point"])
      await flushPromises()

      await wrapper
        .find('button[title="Promote to child note"]')
        .trigger("click")
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      expect(document.body.textContent).toContain("AI is creating note...")
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })

    it("hides LoadingModal when API fails", async () => {
      mockSdkService("promotePoint", wrapSdkError({}))
      const wrapper = mountWithChecklist(["Test understanding point"])
      await flushPromises()

      await wrapper
        .find('button[title="Promote to child note"]')
        .trigger("click")
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      usePopups().popups.done(true)
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })
  })

  describe("LoadingModal for Delete Points", () => {
    it("shows LoadingModal while deleting points", async () => {
      let resolveApi: () => void
      mockSdkServiceWithImplementation("removePointFromNote", async () => {
        await new Promise<void>((r) => {
          resolveApi = r
        })
        return {}
      })
      const wrapper = mountWithChecklist(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      expect(document.body.textContent).toContain("AI is removing content...")
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })

    it("hides LoadingModal when delete API fails", async () => {
      let resolveApi: () => void
      mockSdkServiceWithImplementation("removePointFromNote", async () => {
        await new Promise<void>((r) => {
          resolveApi = r
        })
        return wrapSdkError("API Error")
      })
      const wrapper = mountWithChecklist(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })
  })

  describe("ignore questions", () => {
    it("disables ignore button when no points are selected", async () => {
      const wrapper = mountWithChecklist(["Point 1", "Point 2"])
      await flushPromises()
      expect(
        (
          wrapper.find('[data-test-id="ignore-understanding-points"]')
            .element as HTMLButtonElement
        ).disabled
      ).toBe(true)
    })

    it("enables ignore button when a point is selected", async () => {
      const wrapper = mountWithChecklist(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      expect(
        (
          wrapper.find('[data-test-id="ignore-understanding-points"]')
            .element as HTMLButtonElement
        ).disabled
      ).toBe(false)
    })

    it("shows confirmation when ignore button is clicked", async () => {
      const wrapper = mountWithChecklist(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="ignore-understanding-points"]')
        .trigger("click")
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups).toHaveLength(1)
      expect(popups[0]!.type).toBe("confirm")
      expect(popups[0]!.message).toContain("Ignore")
    })

    it("calls API when confirm is clicked", async () => {
      const ignorePointsSpy = mockSdkService("ignorePoints", { success: true })
      const wrapper = mountWithChecklist(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="ignore-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()

      expect(ignorePointsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { points: ["Point 1"] },
      })
    })

    it("does not call API when cancel is clicked", async () => {
      const ignorePointsSpy = mockSdkService("ignorePoints", { success: true })
      const wrapper = mountWithChecklist(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="ignore-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(false)
      await flushPromises()

      expect(ignorePointsSpy).not.toHaveBeenCalled()
    })
  })

  describe("SpellingVerificationPopup", () => {
    it("closes popup and returns to original state when user closes it", async () => {
      mockSdkService("getNoteInfo", {
        note: makeMe.aNoteRealm.please(),
        createdAt: "",
        noteType: undefined,
        recallSetting: { rememberSpelling: true },
      })
      const wrapper = mount()
      await flushPromises()

      await wrapper.find('[data-test="keep-for-repetition"]').trigger("click")
      await flushPromises()

      expect(document.body.textContent).toContain("Verify Spelling")
      expect(assimilateSpy).not.toHaveBeenCalled()

      const closeButton = document
        .querySelector('[data-test="spelling-verification-popup"]')
        ?.closest(".modal-mask")
        ?.querySelector(".close-button") as HTMLElement
      closeButton.click()
      await flushPromises()

      expect(document.body.textContent).not.toContain("Verify Spelling")
      expect(assimilateSpy).not.toHaveBeenCalled()
      expect(wrapper.find('[data-test="keep-for-repetition"]').exists()).toBe(
        true
      )
    })
  })
})
