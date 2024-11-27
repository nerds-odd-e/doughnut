import RepeatPage from "@/pages/RepeatPage.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { useRouter } from "vue-router"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import mockBrowserTimeZone from "@tests/helpers/mockBrowserTimeZone"

vitest.mock("vue-router", () => ({
  useRouter: () => ({
    currentRoute: {
      value: {
        name: "repeat",
      },
    },
  }),
}))

useRouter().currentRoute.value.name = "repeat"

let renderer: RenderingHelper<typeof RepeatPage>
const mockedRepeatCall = vi.fn()

let teleportTarget: HTMLDivElement

beforeEach(() => {
  teleportTarget = document.createElement("div")
  teleportTarget.id = "head-status"
  document.body.appendChild(teleportTarget)
})
afterEach(() => {
  document.body.innerHTML = ""
})

beforeEach(() => {
  vitest.resetAllMocks()
  helper.managedApi.restNoteController.show1 = vi
    .fn()
    .mockResolvedValue(makeMe.aNote.please())
  helper.managedApi.restReviewsController.repeatReview = mockedRepeatCall
  renderer = helper
    .component(RepeatPage)
    .withStorageProps({ eagerFetchCount: 1 })
})

describe("repeat page", () => {
  const mountPage = async () => {
    const wrapper = renderer.currentRoute({ name: "repeat" }).mount()
    await flushPromises()
    return wrapper
  }

  mockBrowserTimeZone("Asia/Shanghai", beforeEach, afterEach)

  it("redirect to review page if nothing to repeat", async () => {
    const repetition = makeMe.aDueReviewPointsList.please()
    mockedRepeatCall.mockResolvedValue(repetition)
    await mountPage()
    expect(mockedRepeatCall).toHaveBeenCalledWith("Asia/Shanghai", 0)
  })

  describe('repeat page with "just review" quiz', () => {
    const firstReviewPointId = 123
    const secondReviewPointId = 456
    const mockedRandomQuestionCall = vi.fn()
    const mockedReviewPointCall = vi.fn()

    beforeEach(() => {
      vi.useFakeTimers()
      helper.managedApi.restReviewPointController.show =
        mockedReviewPointCall.mockResolvedValue(makeMe.aReviewPoint.please())
      helper.managedApi.silent.restReviewQuestionController.generateRandomQuestion =
        mockedRandomQuestionCall
      mockedRandomQuestionCall.mockRejectedValueOnce(makeMe.anApiError.please())
      mockedRepeatCall.mockResolvedValue(
        makeMe.aDueReviewPointsList
          .toRepeat([firstReviewPointId, secondReviewPointId, 3])
          .please()
      )
    })

    it("shows the progress", async () => {
      await mountPage()
      expect(teleportTarget.textContent).toContain("0/3")
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith(firstReviewPointId)
    })

    it("should show progress", async () => {
      const wrapper = await mountPage()
      const answerResult = makeMe.anAnsweredQuestion
        .withReviewQuestionInstanceId(1)
        .answerCorrect(false)
        .please()
      const mockedMarkAsRepeatedCall = vi.fn().mockResolvedValue(answerResult)
      helper.managedApi.restReviewPointController.markAsRepeated =
        mockedMarkAsRepeatedCall
      const reviewQuestionInstance = makeMe.aReviewQuestionInstance.please()
      mockedRandomQuestionCall.mockResolvedValueOnce(reviewQuestionInstance)
      vi.runOnlyPendingTimers()
      await flushPromises()
      await wrapper.find("button.btn-primary").trigger("click")
      expect(mockedMarkAsRepeatedCall).toHaveBeenCalledWith(
        firstReviewPointId,
        true
      )
      await flushPromises()
      expect(teleportTarget.textContent).toContain("1/3")
      expect(mockedRandomQuestionCall).toHaveBeenCalledWith(secondReviewPointId)
    })

    it("should move current review point to end when requested", async () => {
      const wrapper = await mountPage()

      // Initial order should be [123, 456, 3]
      expect(wrapper.vm.toRepeat).toEqual([123, 456, 3])

      // Click the "Move to end" button
      await wrapper.find('button[title="Move to end of list"]').trigger("click")

      // New order should be [456, 3, 123]
      expect(wrapper.vm.toRepeat).toEqual([456, 3, 123])
    })

    it("should not show move to end button for last item", async () => {
      const wrapper = await mountPage()

      // Move to last item
      wrapper.vm.currentIndex = 2
      await wrapper.vm.$nextTick()

      const quiz = wrapper.findComponent({ name: "Quiz" })
      expect(quiz.vm.canMoveToEnd).toBe(false)
    })
  })
})
