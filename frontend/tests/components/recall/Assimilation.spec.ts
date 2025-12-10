import Assimilation from "@/components/recall/Assimilation.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, {
  mockShowNoteAccessory,
  mockSdkService,
  wrapSdkResponse,
  wrapSdkError,
} from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { ref } from "vue"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useAssimilationCount")

let renderer: RenderingHelper<typeof Assimilation>
let assimilateSpy: ReturnType<typeof mockSdkService<"assimilate">>
let showNoteSpy: ReturnType<typeof mockSdkService<"showNote">>
let updateNoteTypeSpy: ReturnType<typeof mockSdkService<"updateNoteType">>
const mockedIncrementAssimilatedCount = vi.fn()
const mockedTotalAssimilatedCount = ref(0)

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
})

beforeEach(() => {
  assimilateSpy = mockSdkService("assimilate", [])
  showNoteSpy = mockSdkService("showNote", makeMe.aNoteRealm.please())
  updateNoteTypeSpy = mockSdkService("updateNoteType", undefined)
  mockSdkService("getNoteInfo", {
    note: makeMe.aNoteRealm.please(),
    createdAt: "",
  })

  vi.mocked(useRecallData).mockReturnValue({
    totalAssimilatedCount: mockedTotalAssimilatedCount,
    toRepeatCount: ref(0),
    recallWindowEndAt: ref(undefined),
    isRecallPaused: ref(false),
    shouldResumeRecall: ref(false),
    treadmillMode: ref(false),
    currentIndex: ref(0),
    setToRepeatCount: vi.fn(),
    setRecallWindowEndAt: vi.fn(),
    setTotalAssimilatedCount: vi.fn(),
    setIsRecallPaused: vi.fn(),
    resumeRecall: vi.fn(),
    clearShouldResumeRecall: vi.fn(),
    decrementToRepeatCount: vi.fn(),
    setTreadmillMode: vi.fn(),
    setCurrentIndex: vi.fn(),
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

  beforeEach(() => {
    showNoteSpy.mockResolvedValue(wrapSdkResponse(noteRealm))
  })

  describe("normal assimilation", () => {
    it("emits initialReviewDone and increments counts correctly when assimilating normally", async () => {
      const returnedTrackers = [
        { id: 1, removedFromTracking: false },
        { id: 2, removedFromTracking: true },
        { id: 3, removedFromTracking: false },
      ]
      assimilateSpy.mockResolvedValue(wrapSdkResponse(returnedTrackers))
      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note,
        })
        .withRouter()
        .mount()

      await flushPromises()
      await wrapper.find('input[value="Keep for repetition"]').trigger("click")
      await flushPromises()

      expect(assimilateSpy).toHaveBeenCalledWith({
        body: {
          noteId: note.id,
          skipMemoryTracking: false,
        },
      })
      expect(wrapper.emitted()).toHaveProperty("initialReviewDone")
      expect(mockedTotalAssimilatedCount.value).toBe(2)
      expect(mockedIncrementAssimilatedCount).toHaveBeenCalledWith(2)
    })
  })

  describe("note type selection", () => {
    it("always shows note type selection dialog when noteType is missing", async () => {
      const noteWithoutType = {
        ...note,
        noteType: undefined,
      }
      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note: noteWithoutType,
        })
        .withRouter()
        .mount()

      await flushPromises()

      const dialog = wrapper.find('[data-test="note-type-selection-dialog"]')
      expect(dialog.exists()).toBe(true)
      const select = dialog.find("select")
      expect(select.exists()).toBe(true)
      expect(select.element.value).toBe("")
    })

    it("always shows note type selection dialog when noteType is unassigned", async () => {
      const noteWithUnassignedType = {
        ...note,
        noteType: "unassigned" as const,
      }
      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note: noteWithUnassignedType,
        })
        .withRouter()
        .mount()

      await flushPromises()

      const dialog = wrapper.find('[data-test="note-type-selection-dialog"]')
      expect(dialog.exists()).toBe(true)
      const select = dialog.find("select")
      expect(select.element.value).toBe("unassigned")
    })

    it("always shows note type selection dialog even when noteType has a valid value", async () => {
      const noteWithType = {
        ...note,
        noteType: "concept" as const,
      }
      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note: noteWithType,
        })
        .withRouter()
        .mount()

      await flushPromises()

      const dialog = wrapper.find('[data-test="note-type-selection-dialog"]')
      expect(dialog.exists()).toBe(true)
      const select = dialog.find("select")
      expect(select.element.value).toBe("concept")
    })

    it("initializes selectedNoteType from note.noteType when present", async () => {
      const noteWithType = {
        ...note,
        noteType: "category" as const,
      }
      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note: noteWithType,
        })
        .withRouter()
        .mount()

      await flushPromises()

      // Dialog should always be visible and select should show the current value
      const dialog = wrapper.find('[data-test="note-type-selection-dialog"]')
      expect(dialog.exists()).toBe(true)
      const select = dialog.find("select")
      expect(select.element.value).toBe("category")
    })

    it("saves noteType to database when user selects a new type", async () => {
      const noteWithoutType = {
        ...note,
        noteType: undefined,
      }
      updateNoteTypeSpy.mockResolvedValue(wrapSdkResponse(undefined))

      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note: noteWithoutType,
        })
        .withRouter()
        .mount()

      await flushPromises()

      const select = wrapper.find(
        '[data-test="note-type-selection-dialog"] select'
      )
      await select.setValue("concept")
      await flushPromises()

      expect(updateNoteTypeSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: "concept",
      })
    })

    it("reverts selectedNoteType on error when saving fails", async () => {
      const noteWithoutType = {
        ...note,
        noteType: undefined,
      }
      updateNoteTypeSpy.mockResolvedValue(wrapSdkError("Failed to update"))

      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note: noteWithoutType,
        })
        .withRouter()
        .mount()

      await flushPromises()

      const select = wrapper.find(
        '[data-test="note-type-selection-dialog"] select'
      )
      await select.setValue("vocab")
      await flushPromises()

      expect(updateNoteTypeSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: "vocab",
      })
      // After error, the value should revert to empty (original value)
      // Re-find the select after the error to get the updated value
      const selectAfterError = wrapper.find(
        '[data-test="note-type-selection-dialog"] select'
      )
      expect((selectAfterError.element as HTMLSelectElement).value).toBe("")
    })

    it("does not save when selectedNoteType matches current note.noteType", async () => {
      const noteWithType = {
        ...note,
        noteType: "journal" as const,
      }
      const noteWithoutType = {
        ...note,
        noteType: undefined,
      }

      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note: noteWithoutType,
        })
        .withRouter()
        .mount()

      await flushPromises()

      // Set to journal
      const select = wrapper.find(
        '[data-test="note-type-selection-dialog"] select'
      )
      await select.setValue("journal")
      await flushPromises()

      // Clear the mock call count
      updateNoteTypeSpy.mockClear()

      // Update prop to have journal type
      await wrapper.setProps({
        note: noteWithType,
      })
      await flushPromises()

      // The watch on note.noteType should update selectedNoteType,
      // but it should not trigger a save since it matches
      expect(updateNoteTypeSpy).not.toHaveBeenCalled()
    })

    it("updates selectedNoteType when note prop changes", async () => {
      const noteWithoutType = {
        ...note,
        noteType: undefined,
      }
      const noteWithType = {
        ...note,
        noteType: "concept" as const,
      }

      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note: noteWithoutType,
        })
        .withRouter()
        .mount()

      await flushPromises()

      // Initially dialog should be visible
      let dialog = wrapper.find('[data-test="note-type-selection-dialog"]')
      expect(dialog.exists()).toBe(true)
      let select = dialog.find("select")
      expect(select.element.value).toBe("")

      // Update prop to have a type
      await wrapper.setProps({
        note: noteWithType,
      })
      await flushPromises()

      // Dialog should still be visible and select should show the new value
      dialog = wrapper.find('[data-test="note-type-selection-dialog"]')
      expect(dialog.exists()).toBe(true)
      select = dialog.find("select")
      expect(select.element.value).toBe("concept")
    })
  })
})
