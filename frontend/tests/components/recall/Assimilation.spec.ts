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
import { computed, ref } from "vue"
import usePopups from "@/components/commons/Popups/usePopups"

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

  describe("note type selection via NoteInfoBar", () => {
    it("displays note type selection in NoteInfoBar", async () => {
      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note,
        })
        .withRouter()
        .mount()

      await flushPromises()

      // Note type selection is now rendered by NoteInfoComponent via NoteInfoBar
      const noteTypeSelection = wrapper.find(
        '[data-test="note-type-selection-dialog"]'
      )
      expect(noteTypeSelection.exists()).toBe(true)
    })
  })

  describe("promote point to child note", () => {
    it("should display promote button for each understanding point", async () => {
      const points = ["Point 1", "Point 2", "Point 3"]
      mockSdkService("generateUnderstandingChecklist", { points })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // Verify each point has a promote button
      const listItems = wrapper.findAll("li")
      expect(listItems).toHaveLength(3)

      listItems.forEach((li, index) => {
        expect(li.text()).toContain(points[index])
        // Verify button exists
        expect(li.find("button").exists()).toBe(true)
      })
    })

    it("should call promotePoint AI API with parentNoteId when child button is clicked", async () => {
      const points = ["Test Point"]
      mockSdkService("generateUnderstandingChecklist", { points })

      const extractPointSpy = mockSdkService("promotePoint", {
        createdNote: makeMe.aNoteRealm.please(),
        updatedParentNote: makeMe.aNoteRealm.please(),
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // Click the child button (first button in each li)
      await wrapper.find("li button").trigger("click")
      await flushPromises()

      // Verify AI API is called with the point text and parentNoteId
      expect(extractPointSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { point: "Test Point", parentNoteId: note.id },
      })
    })

    it("should remove the point from checklist after successful creation", async () => {
      const points = ["Point 1", "Point 2", "Point 3"]
      mockSdkService("generateUnderstandingChecklist", { points })

      mockSdkService("promotePoint", {
        createdNote: makeMe.aNoteRealm.please(),
        updatedParentNote: makeMe.aNoteRealm.please(),
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // Verify initially 3 points
      expect(wrapper.findAll("li")).toHaveLength(3)

      // Click the second point's button
      const secondLi = wrapper.findAll("li")[1]
      if (secondLi) {
        await secondLi.find("button").trigger("click")
        await flushPromises()
      }

      // Verify only 2 points remain
      expect(wrapper.findAll("li")).toHaveLength(2)
      // Verify "Point 2" is gone
      expect(wrapper.text()).not.toContain("Point 2")
      // Verify the other two remain
      expect(wrapper.text()).toContain("Point 1")
      expect(wrapper.text()).toContain("Point 3")
    })

    it("should keep the point in checklist and show error when AI API fails", async () => {
      const points = ["Test Point"]
      mockSdkService("generateUnderstandingChecklist", { points })

      // Mock API failure
      const extractPointSpy = mockSdkService("promotePoint", undefined)
      extractPointSpy.mockResolvedValue(wrapSdkError("API Error"))

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // Click the button
      await wrapper.find("li button").trigger("click")
      await flushPromises()

      // Verify point remains in the list
      expect(wrapper.findAll("li")).toHaveLength(1)
      expect(wrapper.text()).toContain("Test Point")
    })

    it("should call promotePoint AI API with parent's parentId when sibling button is clicked", async () => {
      const parentNoteRealm = makeMe.aNoteRealm.please()
      const childNoteRealm = makeMe.aNoteRealm.under(parentNoteRealm).please()
      const childMemoryTracker = makeMe.aMemoryTracker
        .ofNote(childNoteRealm)
        .please()
      const childNote = childMemoryTracker.note

      const points = ["Test Point"]
      mockSdkService("generateUnderstandingChecklist", { points })

      const promotePointSpy = mockSdkService("promotePoint", {
        createdNote: makeMe.aNoteRealm.please(),
        updatedParentNote: makeMe.aNoteRealm.please(),
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note: childNote })
        .withRouter()
        .mount()

      await flushPromises()

      // Click the sibling button (second button in the li)
      const buttons = wrapper.findAll("li button")
      await buttons[1]?.trigger("click")
      await flushPromises()

      // Verify AI API is called with the parent's id as parentNoteId
      expect(promotePointSpy).toHaveBeenCalledWith({
        path: { note: childNote.id },
        body: { point: "Test Point", parentNoteId: childNote.parentId },
      })
    })

    it("should hide sibling button when note has no parent", async () => {
      const points = ["Test Point"]
      mockSdkService("generateUnderstandingChecklist", { points })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // Verify sibling button does not exist when note has no parent
      const siblingButton = wrapper.find(
        'button[title="Promote to sibling note"]'
      )
      expect(siblingButton.exists()).toBe(false)

      // Verify child button still exists
      const childButton = wrapper.find('button[title="Promote to child note"]')
      expect(childButton.exists()).toBe(true)
    })
  })

  describe("understanding checklist deletion", () => {
    it("shows checkboxes for each understanding point", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Point 1", "Point 2", "Point 3"],
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // Find checkboxes only within the understanding checklist
      const checkboxes = wrapper
        .find('[data-test-id="understanding-checklist"]')
        .findAll('input[type="checkbox"]')
      expect(checkboxes).toHaveLength(3)
    })

    it("disables delete button when no points are selected", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Point 1", "Point 2"],
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      const deleteButton = wrapper.find(
        '[data-test-id="delete-understanding-points"]'
      )

      // Button should be disabled initially
      expect((deleteButton.element as HTMLButtonElement).disabled).toBe(true)
    })

    it("enables delete button when a point is selected", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Point 1", "Point 2"],
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      const deleteButton = wrapper.find(
        '[data-test-id="delete-understanding-points"]'
      )

      // Check the first item
      const checkboxes = wrapper
        .find('[data-test-id="understanding-checklist"]')
        .findAll('input[type="checkbox"]')
      await checkboxes[0]!.setValue(true)
      await flushPromises()

      // Button should be enabled after selection
      expect((deleteButton.element as HTMLButtonElement).disabled).toBe(false)
    })

    it("shows confirmation dialog when delete button is clicked", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Point 1", "Point 2"],
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // Check an item
      const checkboxes = wrapper
        .find('[data-test-id="understanding-checklist"]')
        .findAll('input[type="checkbox"]')
      await checkboxes[0]!.setValue(true)
      await flushPromises()

      // Click delete button
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()

      // Verify confirmation dialog appears
      const popups = usePopups().popups.peek()
      expect(popups.length).toBe(1)
      expect(popups[0]!.type).toBe("confirm")
      expect(popups[0]!.message).toContain("delete")
    })

    it("calls API and emits reloadNeeded when deletion is confirmed", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Point 1", "Point 2", "Point 3"],
      })

      const deletePointsSpy = mockSdkService("removePointFromNote", {})

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // Check the first item
      const checkboxes = wrapper
        .find('[data-test-id="understanding-checklist"]')
        .findAll('input[type="checkbox"]')
      await checkboxes[0]!.setValue(true)
      await flushPromises()

      // Click delete button
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()

      // Simulate user confirming deletion
      usePopups().popups.done(true)
      await flushPromises()

      // Verify API was called with the point text, not indices
      expect(deletePointsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { points: ["Point 1"] },
      })

      // Verify reloadNeeded event was emitted
      expect(wrapper.emitted()).toHaveProperty("reloadNeeded")
    })

    it("does not call API when deletion is cancelled", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Point 1", "Point 2"],
      })

      const deletePointsSpy = mockSdkService("removePointFromNote", {})

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // Check an item
      const checkboxes = wrapper
        .find('[data-test-id="understanding-checklist"]')
        .findAll('input[type="checkbox"]')
      await checkboxes[0]!.setValue(true)
      await flushPromises()

      // Click delete button
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()

      // Simulate user cancelling deletion
      usePopups().popups.done(false)
      await flushPromises()

      // API should not be called
      expect(deletePointsSpy).not.toHaveBeenCalled()

      // Should not emit reloadNeeded
      expect(wrapper.emitted()).not.toHaveProperty("reloadNeeded")
    })
  })

  describe("LoadingModal for Promote Point", () => {
    it("should show LoadingModal while promoting point to note", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Test understanding point"],
      })

      // Mock with delayed async response to simulate real AI call
      mockSdkServiceWithImplementation("promotePoint", async () => {
        await new Promise((resolve) => setTimeout(resolve, 100))
        return {
          createdNote: makeMe.aNoteRealm.please(),
          updatedParentNote: makeMe.aNoteRealm.please(),
        }
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // Verify understanding point is displayed
      expect(wrapper.text()).toContain("Test understanding point")

      // Click "Child" button
      const childButton = wrapper.find('button[title="Promote to child note"]')
      await childButton.trigger("click")

      // Wait for LoadingModal to appear
      await vi.waitFor(() => {
        expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
        expect(document.body.textContent).toContain("AI is creating note...")
      })

      // Wait for LoadingModal to disappear after API completes
      await vi.waitFor(
        () => {
          expect(document.querySelector(".loading-modal-mask")).toBeNull()
        },
        { timeout: 3000 }
      )
    })

    it("should hide LoadingModal when API call fails", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Test understanding point"],
      })

      // Mock API to fail
      mockSdkService("promotePoint", wrapSdkError({}))

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // Click "Child" button
      const childButton = wrapper.find('button[title="Promote to child note"]')
      await childButton.trigger("click")

      // Wait for LoadingModal to appear
      await vi.waitFor(() => {
        expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      })

      // Wait for error popup and dismiss it
      await vi.waitFor(() => {
        expect(usePopups().popups.peek().length).toBeGreaterThan(0)
      })
      usePopups().popups.done(true)

      // Wait for LoadingModal to disappear
      await vi.waitFor(
        () => {
          expect(document.querySelector(".loading-modal-mask")).toBeNull()
        },
        { timeout: 3000 }
      )
    })
  })

  describe("ignore questions", () => {
    it("disables ignore button when no check points are selected", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Point 1", "Point 2"],
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      const ignoreButton = wrapper.find(
        '[data-test-id="ignore-understanding-points"]'
      )
      expect((ignoreButton.element as HTMLButtonElement).disabled).toBe(true)
    })

    it("enables ignore button when check points are selected", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Point 1", "Point 2"],
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      const ignoreButton = wrapper.find(
        '[data-test-id="ignore-understanding-points"]'
      )

      const checkboxes = wrapper
        .find('[data-test-id="understanding-checklist"]')
        .findAll('input[type="checkbox"]')
      await checkboxes[0]!.setValue(true)
      await flushPromises()

      expect((ignoreButton.element as HTMLButtonElement).disabled).toBe(false)
    })

    it("shows popup when ignore button is clicked", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Point 1", "Point 2"],
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      const checkboxes = wrapper
        .find('[data-test-id="understanding-checklist"]')
        .findAll('input[type="checkbox"]')
      await checkboxes[0]!.setValue(true)
      await flushPromises()

      await wrapper
        .find('[data-test-id="ignore-understanding-points"]')
        .trigger("click")
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups.length).toBe(1)
      expect(popups[0]!.type).toBe("confirm")
      expect(popups[0]!.message).toContain("Ignore")
    })

    it("calls API and emits reloadNeeded when confirm is clicked", async () => {
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Point 1", "Point 2"],
      })

      const ignorePointsSpy = mockSdkService("ignorePoints", { success: true })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      const checkboxes = wrapper
        .find('[data-test-id="understanding-checklist"]')
        .findAll('input[type="checkbox"]')
      await checkboxes[0]!.setValue(true)
      await flushPromises()

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
      mockSdkService("generateUnderstandingChecklist", {
        points: ["Point 1", "Point 2"],
      })

      const ignorePointsSpy = mockSdkService("ignorePoints", { success: true })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      const checkboxes = wrapper
        .find('[data-test-id="understanding-checklist"]')
        .findAll('input[type="checkbox"]')
      await checkboxes[0]!.setValue(true)
      await flushPromises()

      await wrapper
        .find('[data-test-id="ignore-understanding-points"]')
        .trigger("click")
      await flushPromises()

      usePopups().popups.done(false)
      await flushPromises()

      expect(ignorePointsSpy).not.toHaveBeenCalled()
    })
  })
})
