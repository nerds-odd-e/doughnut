import { describe, it, expect } from "vitest"
import type { VueWrapper } from "@vue/test-utils"
import {
  defineComponent,
  ref,
  KeepAlive,
  onActivated,
  onDeactivated,
} from "vue"
import { useThinkingTimeTracker } from "@/composables/useThinkingTimeTracker"
import {
  mountAndFlush,
  setupTrackerClock,
} from "./thinkingTimeTrackerTestSupport"

describe("useThinkingTimeTracker KeepAlive lifecycle", () => {
  const { setTime } = setupTrackerClock()

  const InnerComponent = defineComponent({
    setup() {
      const { start, stop, pause, resume, awayMs, awayCount } =
        useThinkingTimeTracker()
      const result = ref<number | null>(null)

      onActivated(() => {
        start()
        resume()
      })
      onDeactivated(() => pause())

      start()

      const handleStop = () => {
        result.value = stop()
      }

      return { handleStop, result, awayMs, awayCount }
    },
    template: `
      <div>
        <button data-testid="inner-stop" @click="handleStop">Stop</button>
        <span data-testid="inner-result">{{ result }}</span>
        <span data-testid="inner-away-ms">{{ awayMs }}</span>
        <span data-testid="inner-away-count">{{ awayCount }}</span>
      </div>
    `,
  })

  const WrapperComponent = defineComponent({
    components: { InnerComponent, KeepAlive },
    setup() {
      const show = ref(true)
      return { show }
    },
    template: `
      <div>
        <button data-testid="toggle" @click="show = !show">Toggle</button>
        <KeepAlive>
          <InnerComponent v-if="show" key="test" />
        </KeepAlive>
      </div>
    `,
  })

  const mountKeepAliveHarness = () => mountAndFlush(WrapperComponent)

  const innerStopAndExpect = async (wrapper: VueWrapper, expected: string) => {
    await wrapper.get('[data-testid="inner-stop"]').trigger("click")
    expect(wrapper.get('[data-testid="inner-result"]').text()).toBe(expected)
  }

  it("pauses when deactivated", async () => {
    const wrapper = await mountKeepAliveHarness()

    setTime(1000)

    await wrapper.get('[data-testid="toggle"]').trigger("click")
    setTime(2000)

    await wrapper.get('[data-testid="toggle"]').trigger("click")

    await innerStopAndExpect(wrapper, "1000")
  })

  it("resumes when reactivated", async () => {
    const wrapper = await mountKeepAliveHarness()

    setTime(1000)

    await wrapper.get('[data-testid="toggle"]').trigger("click")
    setTime(2000)

    await wrapper.get('[data-testid="toggle"]').trigger("click")

    setTime(3000)

    await innerStopAndExpect(wrapper, "2000")
  })

  it("does not count externally invoked pause/resume as away time", async () => {
    const wrapper = await mountKeepAliveHarness()

    setTime(1000)

    await wrapper.get('[data-testid="toggle"]').trigger("click")
    setTime(2000)

    await wrapper.get('[data-testid="toggle"]').trigger("click")

    expect(wrapper.get('[data-testid="inner-away-ms"]').text()).toBe("0")
    expect(wrapper.get('[data-testid="inner-away-count"]').text()).toBe("0")
  })
})
