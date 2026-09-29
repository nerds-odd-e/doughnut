import { useBookReadingCurrentBlock } from "@/composables/useBookReadingCurrentBlock"
import { mount } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { defineComponent, nextTick, ref } from "vue"

describe("useBookReadingCurrentBlock", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it("reads the reading position when currentBlockId commits after debounce", async () => {
    const propose = vi.fn()
    const Root = defineComponent({
      setup() {
        const notebookId = ref(1)
        const { currentBlockIdDebouncer } = useBookReadingCurrentBlock({
          notebookId,
          readingPosition: () => {
            propose()
            return null
          },
        })
        return { currentBlockIdDebouncer }
      },
      template: "<div />",
    })

    const w = mount(Root)
    const vm = w.vm as {
      currentBlockIdDebouncer: { propose: (id: number) => void }
    }
    vm.currentBlockIdDebouncer.propose(42)
    vi.advanceTimersByTime(120)
    await nextTick()
    expect(propose).toHaveBeenCalledTimes(1)
  })

  it("reads the reading position on commitNow without waiting for debounce", async () => {
    const propose = vi.fn()
    const Root = defineComponent({
      setup() {
        const notebookId = ref(1)
        const { currentBlockIdDebouncer } = useBookReadingCurrentBlock({
          notebookId,
          readingPosition: () => {
            propose()
            return null
          },
        })
        currentBlockIdDebouncer.commitNow(99)
        return {}
      },
      template: "<div />",
    })

    mount(Root)
    await nextTick()
    expect(propose).toHaveBeenCalledTimes(1)
  })
})
