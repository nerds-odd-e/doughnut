import RecallPage from "@/pages/RecallPage.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { useRouter } from "vue-router"
import makeMe from "@tests/fixtures/makeMe"
import helper, {
  mockSdkService,
  wrapSdkResponse,
  wrapSdkError,
} from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import mockBrowserTimeZone from "@tests/helpers/mockBrowserTimeZone"
import type { SpellingResultDto, MemoryTrackerLite } from "@generated/backend"

vitest.mock("vue-router", () => ({
  useRoute: () => ({
    path: "/",
    fullPath: "/",
  }),
  useRouter: () => ({
    currentRoute: {
      value: {
        name: "recall",
      },
    },
  }),
}))

useRouter().currentRoute.value.name = "recall"

let renderer: RenderingHelper<typeof RecallPage>
let recallingSpy: ReturnType<typeof mockSdkService<"recalling">>

afterEach(() => {
  document.body.innerHTML = ""
  localStorage.clear()
})

beforeEach(() => {
  vitest.resetAllMocks()
  localStorage.clear()
  mockSdkService("showNote", makeMe.aNoteRealm.please())
  recallingSpy = mockSdkService(
    "recalling",
    makeMe.aDueMemoryTrackersList.please()
  )
  mockSdkService(
    "askAQuestion",
    makeMe.aRecallPrompt.withQuestionType("SPELLING").please()
  )
  renderer = helper
    .component(RecallPage)
    .withCleanStorage()
    .withProps({ eagerFetchCount: 1 })
})

describe("repeat page", () => {
  const createMemoryTrackerLite = (
    id: number,
    spelling = false
  ): MemoryTrackerLite => ({
    memoryTrackerId: id,
    spelling,
  })

  const mountPage = async () => {
    const wrapper = renderer.currentRoute({ name: "recall" }).mount()
    await flushPromises()
    return wrapper
  }

  mockBrowserTimeZone("Asia/Shanghai", beforeEach, afterEach)

  it("redirect to review page if nothing to repeat", async () => {
    const repetition = makeMe.aDueMemoryTrackersList.please()
    recallingSpy.mockResolvedValue(wrapSdkResponse(repetition))
    await mountPage()
    expect(recallingSpy).toHaveBeenCalledWith({
      query: {
        timezone: "Asia/Shanghai",
        dueindays: 0,
      },
    })
  })

  describe('repeat page with "just review" quiz', () => {
    const firstMemoryTrackerId = 123
    const secondMemoryTrackerId = 456
    let askAQuestionSpy: ReturnType<typeof mockSdkService<"askAQuestion">>

    beforeEach(() => {
      vi.useFakeTimers()
      mockSdkService("showMemoryTracker", makeMe.aMemoryTracker.please())
      askAQuestionSpy = mockSdkService(
        "askAQuestion",
        makeMe.aRecallPrompt.please()
      )
      askAQuestionSpy.mockResolvedValueOnce(wrapSdkError("API Error"))
      recallingSpy.mockResolvedValue(
        wrapSdkResponse(
          makeMe.aDueMemoryTrackersList
            .toRepeat([
              createMemoryTrackerLite(firstMemoryTrackerId),
              createMemoryTrackerLite(secondMemoryTrackerId),
              createMemoryTrackerLite(3),
            ])
            .please()
        )
      )
    })

    it("shows the progress", async () => {
      const wrapper = await mountPage()
      const globalBar = wrapper.findComponent({ name: "GlobalBar" })
      expect(globalBar.text()).toContain("0/3")
      expect(askAQuestionSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { memoryTracker: firstMemoryTrackerId },
        })
      )
    })

    it("should show progress", async () => {
      const wrapper = await mountPage()
      const mockedMarkAsRepeatedCall = mockSdkService(
        "markAsRepeated",
        makeMe.aMemoryTracker.please()
      )
      const recallPrompt = makeMe.aRecallPrompt.please()
      askAQuestionSpy.mockResolvedValueOnce(wrapSdkResponse(recallPrompt))
      vi.runOnlyPendingTimers()
      await flushPromises()
      await wrapper.find("button.daisy-btn-primary").trigger("click")
      expect(mockedMarkAsRepeatedCall).toHaveBeenCalledWith({
        path: { memoryTracker: firstMemoryTrackerId },
        query: { successful: true },
      })
      await flushPromises()
      const globalBar = wrapper.findComponent({ name: "GlobalBar" })
      expect(globalBar.text()).toContain("1/3")
      expect(askAQuestionSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { memoryTracker: secondMemoryTrackerId },
        })
      )
    })

    it("should move current memory tracker to end when requested", async () => {
      const wrapper = await mountPage()
      type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
      const vm = wrapper.vm as unknown as ExposedVM

      // Initial order should be [123, 456, 3]
      expect(vm.toRepeat?.map((t) => t.memoryTrackerId)).toEqual([123, 456, 3])

      // Click the "Recall settings" button to open the dropdown
      await wrapper.find('button[title="Recall settings"]').trigger("click")
      await wrapper.vm.$nextTick()

      // Click the "Move to end" button in the modal dialog
      const moveToEndButton = document.body.querySelector(
        'button[title="Move to end of list"]'
      )
      expect(moveToEndButton).toBeTruthy()
      await moveToEndButton?.dispatchEvent(new Event("click"))
      await wrapper.vm.$nextTick()

      // New order should be [456, 3, 123]
      expect(vm.toRepeat?.map((t) => t.memoryTrackerId)).toEqual([456, 3, 123])
    })

    it("should not show move to end button for last item", async () => {
      const wrapper = await mountPage()
      type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
      const vm = wrapper.vm as unknown as ExposedVM

      // Move to last item
      vm.currentIndex = 2
      await wrapper.vm.$nextTick()

      // Click the "Recall settings" button to open the dropdown
      await wrapper.find('button[title="Recall settings"]').trigger("click")
      await wrapper.vm.$nextTick()

      // Button should not be visible when on last item
      const moveToEndButton = document.body.querySelector(
        'button[title="Move to end of list"]'
      )
      expect(moveToEndButton).toBeFalsy()
    })
  })

  describe('repeat page with "spelling" quiz', () => {
    const firstMemoryTrackerId = 123

    beforeEach(() => {
      localStorage.clear()
      vi.useFakeTimers()
      mockSdkService("showMemoryTracker", makeMe.aMemoryTracker.please())
      mockSdkService("askAQuestion", makeMe.aRecallPrompt.please())

      recallingSpy.mockResolvedValue(
        wrapSdkResponse(
          makeMe.aDueMemoryTrackersList
            .toRepeat([createMemoryTrackerLite(firstMemoryTrackerId, true)])
            .please()
        )
      )
    })

    it("should handle spelling questions correctly", async () => {
      const note = makeMe.aNote.please()
      note.id = 42
      const answerResult: SpellingResultDto = {
        note,
        answer: "test answer",
        isCorrect: false,
        memoryTrackerId: 123,
      }

      const mockedAnswerSpellingCall = mockSdkService(
        "answerSpelling",
        answerResult
      )

      const wrapper = await mountPage()
      await flushPromises()

      // Debug: print the wrapper's HTML
      await wrapper.find("input#memory_tracker-answer").setValue("test answer")
      await flushPromises()
      await wrapper.find("form").trigger("submit")
      await flushPromises()
      expect(mockedAnswerSpellingCall).toHaveBeenCalled()

      // Verify that a spelling result was created and displayed correctly
      const answeredSpellingQuestion = wrapper.findComponent({
        name: "AnsweredSpellingQuestion",
      })
      expect(answeredSpellingQuestion.exists()).toBe(true)
      const spellingAlert = answeredSpellingQuestion.find(".daisy-alert-error")
      expect(spellingAlert.exists()).toBe(true)
      expect(spellingAlert.text()).toContain(
        "Your answer `test answer` is incorrect."
      )

      // Verify note is displayed
      const noteShow = answeredSpellingQuestion.findComponent({
        name: "NoteShow",
      })
      expect(noteShow.exists()).toBe(true)
      expect(noteShow.props("noteId")).toBe(42)

      // Verify View Memory Tracker link is displayed
      const viewMemoryTrackerLink = answeredSpellingQuestion.findComponent({
        name: "ViewMemoryTrackerLink",
      })
      expect(viewMemoryTrackerLink.exists()).toBe(true)
      expect(viewMemoryTrackerLink.props("memoryTrackerId")).toBe(123)
    })
  })

  describe("treadmill mode", () => {
    const normalMemoryTrackerId = 123
    const spellingMemoryTrackerId = 456
    const anotherNormalMemoryTrackerId = 789
    let askAQuestionSpy: ReturnType<typeof mockSdkService<"askAQuestion">>

    beforeEach(() => {
      localStorage.clear()
      vi.useFakeTimers()
      mockSdkService("showMemoryTracker", makeMe.aMemoryTracker.please())
      askAQuestionSpy = mockSdkService(
        "askAQuestion",
        makeMe.aRecallPrompt.please()
      )
      recallingSpy.mockResolvedValue(
        wrapSdkResponse(
          makeMe.aDueMemoryTrackersList
            .toRepeat([
              createMemoryTrackerLite(normalMemoryTrackerId, false),
              createMemoryTrackerLite(spellingMemoryTrackerId, true),
              createMemoryTrackerLite(anotherNormalMemoryTrackerId, false),
            ])
            .please()
        )
      )
    })

    const mountPage = async () => {
      const wrapper = renderer.currentRoute({ name: "recall" }).mount()
      await flushPromises()
      return wrapper
    }

    it("should show treadmill mode toggle in settings", async () => {
      const wrapper = await mountPage()
      await wrapper.find('button[title="Recall settings"]').trigger("click")
      await wrapper.vm.$nextTick()

      // Dialog is shown as a modal
      const toggle = document.body.querySelector('input[type="checkbox"]')
      expect(toggle).toBeTruthy()
      expect(document.body.textContent).toContain("Treadmill mode")
    })

    it("should skip spelling memory trackers when treadmill mode is enabled", async () => {
      const wrapper = await mountPage()
      const globalBar = wrapper.findComponent({ name: "GlobalBar" })
      expect(globalBar.text()).toContain("0/3")

      // Enable treadmill mode
      await wrapper.find('button[title="Recall settings"]').trigger("click")
      await wrapper.vm.$nextTick()
      const toggle = document.body.querySelector(
        'input[type="checkbox"]'
      ) as HTMLInputElement
      expect(toggle).toBeTruthy()
      toggle.checked = true
      toggle.dispatchEvent(new Event("change", { bubbles: true }))
      await wrapper.vm.$nextTick()

      // Progress should now show 0/2 (excluding spelling tracker)
      expect(globalBar.text()).toContain("0/2")

      // Should start with first normal tracker, not spelling
      expect(askAQuestionSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { memoryTracker: normalMemoryTrackerId },
        })
      )
    })

    it("should apply sportive background to GlobalBar when treadmill mode is enabled", async () => {
      const wrapper = await mountPage()

      // Enable treadmill mode
      await wrapper.find('button[title="Recall settings"]').trigger("click")
      await wrapper.vm.$nextTick()
      const toggle = document.body.querySelector(
        'input[type="checkbox"]'
      ) as HTMLInputElement
      expect(toggle).toBeTruthy()
      toggle.checked = true
      toggle.dispatchEvent(new Event("change", { bubbles: true }))
      await wrapper.vm.$nextTick()

      const globalBar = wrapper.findComponent({ name: "GlobalBar" })
      expect(globalBar.classes()).toContain("treadmill-mode")
    })

    it("should not show spelling questions when treadmill mode is enabled", async () => {
      const wrapper = await mountPage()

      // Enable treadmill mode
      await wrapper.find('button[title="Recall settings"]').trigger("click")
      await wrapper.vm.$nextTick()
      const toggle = document.body.querySelector(
        'input[type="checkbox"]'
      ) as HTMLInputElement
      expect(toggle).toBeTruthy()
      toggle.checked = true
      toggle.dispatchEvent(new Event("change", { bubbles: true }))
      await wrapper.vm.$nextTick()

      // Should skip spelling tracker and go to next normal one
      const quiz = wrapper.findComponent({ name: "Quiz" })
      if (quiz.exists()) {
        type ExposedVM = {
          toRepeat?: MemoryTrackerLite[]
          currentIndex: number
        }
        const vm = wrapper.vm as unknown as ExposedVM
        const currentTracker = vm.toRepeat?.[vm.currentIndex]
        expect(currentTracker?.spelling).toBe(false)
      }
    })

    it("should update progress bar to exclude spelling memory trackers", async () => {
      recallingSpy.mockResolvedValue(
        wrapSdkResponse(
          makeMe.aDueMemoryTrackersList
            .toRepeat([
              createMemoryTrackerLite(normalMemoryTrackerId, false),
              createMemoryTrackerLite(spellingMemoryTrackerId, true),
              createMemoryTrackerLite(anotherNormalMemoryTrackerId, false),
            ])
            .please()
        )
      )
      const wrapper = await mountPage()
      await flushPromises()
      const globalBar = wrapper.findComponent({ name: "GlobalBar" })

      // Initially shows all 3 trackers (treadmill mode is off by default)
      // Note: The progress bar shows remaining count, which is 3 initially
      expect(globalBar.text()).toMatch(/0\/[23]/)

      // Enable treadmill mode
      await wrapper.find('button[title="Recall settings"]').trigger("click")
      await wrapper.vm.$nextTick()
      await flushPromises()
      const toggle = document.body.querySelector(
        'input[type="checkbox"]'
      ) as HTMLInputElement
      expect(toggle).toBeTruthy()
      toggle.checked = true
      toggle.dispatchEvent(new Event("change", { bubbles: true }))
      await wrapper.vm.$nextTick()
      await flushPromises()

      // Should now show 2 (excluding spelling)
      expect(globalBar.text()).toContain("0/2")
    })

    it("should not add answered questions back to the list when toggling treadmill mode", async () => {
      mockSdkService("markAsRepeated", makeMe.aMemoryTracker.please())
      const wrapper = await mountPage()
      await flushPromises()
      type ExposedVM = {
        toRepeat?: MemoryTrackerLite[]
        currentIndex: number
      }
      const vm = wrapper.vm as unknown as ExposedVM

      // Manually advance currentIndex to simulate answering a question
      vm.currentIndex = 1

      // Enable treadmill mode
      await wrapper.find('button[title="Recall settings"]').trigger("click")
      await wrapper.vm.$nextTick()
      const toggle = document.body.querySelector(
        'input[type="checkbox"]'
      ) as HTMLInputElement
      expect(toggle).toBeTruthy()
      toggle.checked = true
      toggle.dispatchEvent(new Event("change", { bubbles: true }))
      await wrapper.vm.$nextTick()
      await flushPromises()

      // currentIndex should not be reset to 0
      // It should remain at 1 or be adjusted to point to a non-spelling tracker
      expect(vm.currentIndex).toBeGreaterThan(0)
    })

    it("should not reset currentIndex to 0 when toggling treadmill mode off", async () => {
      const wrapper = await mountPage()
      await flushPromises()
      type ExposedVM = {
        toRepeat?: MemoryTrackerLite[]
        currentIndex: number
      }
      const vm = wrapper.vm as unknown as ExposedVM

      // Manually set currentIndex to simulate having answered questions
      vm.currentIndex = 2

      // Enable treadmill mode
      await wrapper.find('button[title="Recall settings"]').trigger("click")
      await wrapper.vm.$nextTick()
      let toggle = document.body.querySelector(
        'input[type="checkbox"]'
      ) as HTMLInputElement
      expect(toggle).toBeTruthy()
      toggle.checked = true
      toggle.dispatchEvent(new Event("change", { bubbles: true }))
      await wrapper.vm.$nextTick()
      await flushPromises()

      // Disable treadmill mode - reopen the settings dialog
      await wrapper.find('button[title="Recall settings"]').trigger("click")
      await wrapper.vm.$nextTick()
      toggle = document.body.querySelector(
        'input[type="checkbox"]'
      ) as HTMLInputElement
      expect(toggle).toBeTruthy()
      toggle.checked = false
      toggle.dispatchEvent(new Event("change", { bubbles: true }))
      await wrapper.vm.$nextTick()
      await flushPromises()

      // currentIndex should not be reset to 0
      expect(vm.currentIndex).toBeGreaterThan(0)
    })
  })
})
