import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect } from "vitest"
import helper from "@tests/helpers"
import RecallSessionOptionsDialog from "@/components/recall/RecallSessionOptionsDialog.vue"
import makeMe from "@tests/fixtures/makeMe"
import type { RecallPrompt } from "@generated/backend"

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
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
  }
})

vi.mock("@/composables/useRecallData", () => ({
  useRecallData: () => ({
    treadmillMode: { value: false },
    setTreadmillMode: vi.fn(),
  }),
}))

describe("RecallSessionOptionsDialog", () => {
  const defaultProps = {
    canMoveToEnd: false,
    previousAnsweredQuestionCursor: undefined,
    currentIndex: 0,
    finished: 0,
    toRepeatCount: 0,
    totalAssimilatedCount: 0,
    previousAnsweredQuestions: [] as (RecallPrompt | undefined)[],
  }

  const mountWithTeleportStub = (
    component: typeof RecallSessionOptionsDialog,
    props: typeof defaultProps
  ) =>
    helper
      .component(component)
      .withProps(props)
      .mount({
        global: {
          stubs: {
            Teleport: true,
          },
        },
      })

  it("displays average thinking time when there are MCQ questions with thinking time", async () => {
    const note = makeMe.aNote.please()
    const predefinedQuestion = makeMe.aPredefinedQuestion.please()
    const questionResult1: RecallPrompt = makeMe.aRecallPrompt
      .withId(1)
      .withNote(note)
      .withPredefinedQuestion(predefinedQuestion)
      .withAnswer({
        id: 1,
        correct: true,
        choiceIndex: 0,
        thinkingTimeMs: 5000,
      })
      .withMemoryTrackerId(1)
      .please()
    const questionResult2: RecallPrompt = makeMe.aRecallPrompt
      .withId(2)
      .withNote(note)
      .withPredefinedQuestion(predefinedQuestion)
      .withAnswer({
        id: 2,
        correct: true,
        choiceIndex: 1,
        thinkingTimeMs: 3000,
      })
      .withMemoryTrackerId(2)
      .please()

    const wrapper = mountWithTeleportStub(RecallSessionOptionsDialog, {
      ...defaultProps,
      previousAnsweredQuestions: [questionResult1, questionResult2],
    })

    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain("Average thinking time: 4s")
  })

  it("does not display average thinking time when there are no MCQ questions", async () => {
    const note = makeMe.aNote.please()
    const spellingResult: RecallPrompt = makeMe.aRecallPrompt
      .withNote(note)
      .withQuestionType("SPELLING")
      .withAnswer({ id: 1, correct: true, spellingAnswer: "test" })
      .withMemoryTrackerId(1)
      .please()

    const wrapper = mountWithTeleportStub(RecallSessionOptionsDialog, {
      ...defaultProps,
      previousAnsweredQuestions: [spellingResult],
    })

    await flushPromises()

    const text = wrapper.text()
    expect(text).not.toContain("Average thinking time")
  })

  it("does not display average thinking time when MCQ questions have no thinking time", async () => {
    const note = makeMe.aNote.please()
    const questionResult: RecallPrompt = makeMe.aRecallPrompt
      .withId(1)
      .withNote(note)
      .withPredefinedQuestion(makeMe.aPredefinedQuestion.please())
      .withAnswer({ id: 1, correct: true, choiceIndex: 0 })
      .withMemoryTrackerId(1)
      .please()

    const wrapper = mountWithTeleportStub(RecallSessionOptionsDialog, {
      ...defaultProps,
      previousAnsweredQuestions: [questionResult],
    })

    await flushPromises()

    const text = wrapper.text()
    expect(text).not.toContain("Average thinking time")
  })

  it("formats thinking time correctly for milliseconds", async () => {
    const note = makeMe.aNote.please()
    const questionResult: RecallPrompt = makeMe.aRecallPrompt
      .withId(1)
      .withNote(note)
      .withPredefinedQuestion(makeMe.aPredefinedQuestion.please())
      .withAnswer({
        id: 1,
        correct: true,
        choiceIndex: 0,
        thinkingTimeMs: 500,
      })
      .withMemoryTrackerId(1)
      .please()

    const wrapper = mountWithTeleportStub(RecallSessionOptionsDialog, {
      ...defaultProps,
      previousAnsweredQuestions: [questionResult],
    })

    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain("Average thinking time: 500ms")
  })

  it("formats thinking time correctly for minutes and seconds", async () => {
    const note = makeMe.aNote.please()
    const questionResult: RecallPrompt = makeMe.aRecallPrompt
      .withId(1)
      .withNote(note)
      .withPredefinedQuestion(makeMe.aPredefinedQuestion.please())
      .withAnswer({
        id: 1,
        correct: true,
        choiceIndex: 0,
        thinkingTimeMs: 125000,
      })
      .withMemoryTrackerId(1)
      .please()

    const wrapper = mountWithTeleportStub(RecallSessionOptionsDialog, {
      ...defaultProps,
      previousAnsweredQuestions: [questionResult],
    })

    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain("Average thinking time: 2m 5s")
  })

  it("filters out undefined results when calculating average", async () => {
    const note = makeMe.aNote.please()
    const questionResult: RecallPrompt = makeMe.aRecallPrompt
      .withId(1)
      .withNote(note)
      .withPredefinedQuestion(makeMe.aPredefinedQuestion.please())
      .withAnswer({
        id: 1,
        correct: true,
        choiceIndex: 0,
        thinkingTimeMs: 6000,
      })
      .withMemoryTrackerId(1)
      .please()

    const wrapper = mountWithTeleportStub(RecallSessionOptionsDialog, {
      ...defaultProps,
      previousAnsweredQuestions: [questionResult, undefined],
    })

    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain("Average thinking time: 6s")
  })
})
