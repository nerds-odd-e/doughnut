import { flushPromises } from "@vue/test-utils"
import { vi, beforeEach, afterEach, describe, it, expect } from "vitest"
import { defineComponent, KeepAlive, nextTick } from "vue"
import helper from "@tests/helpers"
import QuestionDisplay from "@/components/recall/QuestionDisplay.vue"
import makeMe from "donut-test-fixtures/makeMe"
import { questionDisplayProps } from "./questionDisplayTestSupport"

describe("QuestionDisplay thinking time", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>
  let rafCallbacks: Array<FrameRequestCallback> = []

  beforeEach(() => {
    vi.useFakeTimers()
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
    rafCallbacks = []
    vi.spyOn(window, "requestAnimationFrame").mockImplementation(
      (callback: FrameRequestCallback) => {
        rafCallbacks.push(callback)
        return 1
      }
    )
  })

  afterEach(() => {
    Object.defineProperty(document, "hidden", { value: false, writable: true })
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  const flushRAF = () => {
    const callbacks = [...rafCallbacks]
    rafCallbacks = []
    callbacks.forEach((cb) => cb(performance.now()))
  }

  const setTime = (ms: number) => {
    performanceNowSpy.mockReturnValue(ms)
    vi.advanceTimersByTime(ms)
  }

  const activeQuestion = () =>
    makeMe.anMcq
      .withQuestionStem("Test question")
      .withChoices(["A", "B", "C"])
      .please()

  async function mountActiveQuestion() {
    const wrapper = helper
      .component(QuestionDisplay)
      .withProps(questionDisplayProps(activeQuestion()))
      .mount()
    await flushPromises()
    flushRAF()
    return wrapper
  }

  async function mountKeepAliveQuestion(
    mcq: ReturnType<typeof activeQuestion>
  ) {
    const WrapperComponent = defineComponent({
      components: { QuestionDisplay, KeepAlive },
      data() {
        return { show: true, question: mcq }
      },
      template: `
        <KeepAlive>
          <QuestionDisplay
            v-if="show"
            key="question"
            :questionStem="question.questionStem"
            :responseChoices="question.responseChoices"
          />
        </KeepAlive>
      `,
    })

    const wrapper = helper.component(WrapperComponent).mount()
    await flushPromises()

    const questionComponent = wrapper.findComponent(QuestionDisplay)
    await questionComponent.vm.$nextTick()
    flushRAF()

    return { wrapper, questionComponent }
  }

  it("includes thinking time in answer submission", async () => {
    const wrapper = await mountActiveQuestion()
    setTime(5000)

    await wrapper.find("li.choice button").trigger("click")
    await flushPromises()

    const answerData = wrapper.emitted("answer")?.[0]?.[0] as {
      thinkingTimeMs?: number
    }
    expect(answerData?.thinkingTimeMs).toBeGreaterThanOrEqual(5000)
  })

  it("only records thinking time once per submission", async () => {
    const wrapper = await mountActiveQuestion()
    setTime(1000)

    await wrapper.find("li.choice button").trigger("click")
    await flushPromises()

    const answerData = wrapper.emitted("answer")?.[0]?.[0] as {
      thinkingTimeMs?: number
    }
    expect(answerData?.thinkingTimeMs).toBe(1000)
  })

  it("shows inactive mask when document is hidden", async () => {
    const wrapper = await mountActiveQuestion()

    expect(wrapper.find("[data-test='inactive-recall-mask']").exists()).toBe(
      false
    )

    Object.defineProperty(document, "hidden", { value: true, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))
    await nextTick()

    const mask = wrapper.find("[data-test='inactive-recall-mask']")
    expect(mask.exists()).toBe(true)
    expect(mask.text()).toContain("Focus to activate")
  })

  it("shows inactive mask when window loses focus", async () => {
    const wrapper = await mountActiveQuestion()

    expect(wrapper.find("[data-test='inactive-recall-mask']").exists()).toBe(
      false
    )

    window.dispatchEvent(new Event("blur"))
    await nextTick()

    expect(wrapper.find("[data-test='inactive-recall-mask']").exists()).toBe(
      true
    )
  })

  it("hides inactive mask when window regains focus", async () => {
    const wrapper = await mountActiveQuestion()

    window.dispatchEvent(new Event("blur"))
    await nextTick()
    expect(wrapper.find("[data-test='inactive-recall-mask']").exists()).toBe(
      true
    )

    window.dispatchEvent(new Event("focus"))
    await nextTick()
    expect(wrapper.find("[data-test='inactive-recall-mask']").exists()).toBe(
      false
    )
  })

  it("records a detour when deactivated and reactivated (KeepAlive)", async () => {
    const { wrapper, questionComponent } = await mountKeepAliveQuestion(
      activeQuestion()
    )
    setTime(1000)

    await wrapper.setData({ show: false })
    await wrapper.vm.$nextTick()
    setTime(2500)

    await wrapper.setData({ show: true })
    await questionComponent.vm.$nextTick()
    flushRAF()
    setTime(3000)

    await questionComponent.find("li.choice button").trigger("click")
    await flushPromises()

    const answerData = questionComponent.emitted("answer")?.[0]?.[0] as {
      detourMs?: number
      detourCount?: number
    }
    expect(answerData?.detourCount).toBe(1)
    expect(answerData?.detourMs).toBeGreaterThanOrEqual(1500)
  })
})
