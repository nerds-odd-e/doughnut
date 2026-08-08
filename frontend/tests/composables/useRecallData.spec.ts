import { useRecallData } from "@/composables/useRecallData"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  expectSoftKeyboardPrimerIsFocused,
  expectSoftKeyboardPrimerIsNotFocused,
  mountSoftKeyboardPrimer,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import { mount } from "@vue/test-utils"
import { afterEach, describe, expect, it } from "vitest"
import { defineComponent } from "vue"
import { createMemoryHistory, createRouter } from "vue-router"

const ResumeHarness = defineComponent({
  setup() {
    const { resumeRecall } = useRecallData()
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

function mountResumeHarness() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: "/", component: { template: "<div />" } },
      {
        path: "/recall",
        name: "recall",
        component: { template: "<div />" },
      },
    ],
  })
  return mount(ResumeHarness, {
    global: { plugins: [router] },
  })
}

describe("useRecallData resumeRecall", () => {
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
    document.body.innerHTML = ""
  })

  it.each([
    { coarse: true, expectPrimerFocused: true },
    { coarse: false, expectPrimerFocused: false },
  ])(
    "primes soft keyboard on resume when coarse=$coarse",
    ({ coarse, expectPrimerFocused }) => {
      matchMediaSpy = mockCoarsePointer(coarse)
      mountSoftKeyboardPrimer()
      const wrapper = mountResumeHarness()

      wrapper.find("button").trigger("click")

      if (expectPrimerFocused) {
        expectSoftKeyboardPrimerIsFocused()
      } else {
        expectSoftKeyboardPrimerIsNotFocused()
      }
      wrapper.unmount()
    }
  )
})

describe("useRecallData potentialLearningSessions", () => {
  it("groups dueCommissioned by notebookId without affecting toRepeatCount", () => {
    const wrapper = mount(PotentialSessionHarness, {
      global: {
        plugins: [
          createRouter({
            history: createMemoryHistory(),
            routes: [{ path: "/", component: { template: "<div />" } }],
          }),
        ],
      },
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
