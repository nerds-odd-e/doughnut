import { useRecallData } from "@/composables/useRecallData"
import { useResumeRecall } from "@/composables/useResumeRecall"
import { dummyRouteRecordsFromMetadata } from "@/routes/dummyRouteRecords"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  expectSoftKeyboardPrimerIsFocused,
  expectSoftKeyboardPrimerIsNotFocused,
  mountSoftKeyboardPrimer,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import { flushPromises, mount } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { defineComponent } from "vue"
import { createMemoryHistory, createRouter } from "vue-router"

function createRecallDataTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: dummyRouteRecordsFromMetadata,
  })
}

const ResumeHarness = defineComponent({
  props: {
    spelling: { type: Boolean, default: true },
    treadmillMode: { type: Boolean, default: false },
  },
  setup(props) {
    const { setToRepeat, setCurrentIndex, setTreadmillMode } = useRecallData()
    const { resumeRecall } = useResumeRecall()
    setToRepeat([{ memoryTrackerId: 1, spelling: props.spelling }])
    setCurrentIndex(0)
    setTreadmillMode(props.treadmillMode)
    return { resumeRecall }
  },
  template: `<button type="button" @click="resumeRecall">Resume</button>`,
})

const PotentialSessionHarness = defineComponent({
  setup() {
    const { setDueCommissioned, potentialLearningSessions, toRepeatCount } =
      useRecallData()
    return { setDueCommissioned, potentialLearningSessions, toRepeatCount }
  },
  template: `<div />`,
})

function mountResumeHarness(options?: {
  spelling?: boolean
  treadmillMode?: boolean
}) {
  return mount(ResumeHarness, {
    props: {
      spelling: options?.spelling ?? true,
      treadmillMode: options?.treadmillMode ?? false,
    },
    global: { plugins: [createRecallDataTestRouter()] },
  })
}

function resetRecallDataState() {
  const ResetHarness = defineComponent({
    setup() {
      const {
        setToRepeat,
        setCurrentIndex,
        setTreadmillMode,
        clearShouldResumeRecall,
      } = useRecallData()
      setToRepeat(undefined)
      setCurrentIndex(0)
      setTreadmillMode(false)
      clearShouldResumeRecall()
      return () => null
    },
    template: "<div />",
  })
  const wrapper = mount(ResetHarness, {
    global: { plugins: [createRecallDataTestRouter()] },
  })
  wrapper.unmount()
}

describe("useResumeRecall", () => {
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
    document.body.innerHTML = ""
    resetRecallDataState()
  })

  it.each([
    {
      coarse: true,
      spelling: true,
      treadmillMode: false,
      expectPrimerFocused: true,
    },
    {
      coarse: true,
      spelling: false,
      treadmillMode: false,
      expectPrimerFocused: false,
    },
    {
      coarse: true,
      spelling: true,
      treadmillMode: true,
      expectPrimerFocused: false,
    },
    {
      coarse: false,
      spelling: true,
      treadmillMode: false,
      expectPrimerFocused: false,
    },
  ])(
    "primes soft keyboard on resume when coarse=$coarse spelling=$spelling treadmill=$treadmillMode",
    ({ coarse, spelling, treadmillMode, expectPrimerFocused }) => {
      matchMediaSpy = mockCoarsePointer(coarse)
      mountSoftKeyboardPrimer()
      const wrapper = mountResumeHarness({ spelling, treadmillMode })

      wrapper.find("button").trigger("click")

      if (expectPrimerFocused) {
        expectSoftKeyboardPrimerIsFocused()
      } else {
        expectSoftKeyboardPrimerIsNotFocused()
      }
      wrapper.unmount()
    }
  )

  it("navigates to named recall on resume", async () => {
    const wrapper = mountResumeHarness()

    await wrapper.find("button").trigger("click")
    await flushPromises()

    expect(wrapper.vm.$router.currentRoute.value.name).toBe("recall")
    wrapper.unmount()
  })
})

describe("useRecallData potentialLearningSessions", () => {
  beforeEach(() => {
    resetRecallDataState()
  })

  it("groups dueCommissioned by notebookId without affecting toRepeatCount", () => {
    const wrapper = mount(PotentialSessionHarness, {
      global: { plugins: [createRecallDataTestRouter()] },
    })
    wrapper.vm.setDueCommissioned([
      {
        memoryTrackerId: 1,
        notebookId: 10,
        notebookName: "Spanish conversation",
      },
      {
        memoryTrackerId: 2,
        notebookId: 10,
        notebookName: "Spanish conversation",
      },
    ])
    expect(wrapper.vm.potentialLearningSessions).toEqual([
      {
        notebookId: 10,
        notebookName: "Spanish conversation",
      },
    ])
    expect(wrapper.vm.toRepeatCount).toBe(0)
    wrapper.unmount()
  })
})
