import {
  NoteController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import type {
  AnswerSpellingData,
  GetRecallPromptData,
  ShowNoteData,
} from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"
import { nextTick, ref } from "vue"
import { focusDirective } from "@/utils/focusTarget"
import {
  captureRequestAnimationFrame,
  flushCapturedAnimationFrames,
} from "@tests/components/recall/spellingQuestionDisplayTestSupport"
import { useRecallPageSpecContext } from "./recallPageTestSupport"
import {
  givenSpellingRecoveryQueue,
  input,
  retry,
  progress,
  result,
  submit,
} from "./spellingRecoveryTestSupport"

const ctx = useRecallPageSpecContext({ fakeTimers: true })
let fetchPrompt: ReturnType<typeof givenSpellingRecoveryQueue>
beforeEach(() => {
  fetchPrompt = givenSpellingRecoveryQueue()
})

describe("RecallPage recovered spelling outcomes", () => {
  it("recovers overlap then starts a distinct more specific answer on the same tracker with a clear focused input", async () => {
    const frames = captureRequestAnimationFrame()
    const accepted = makeMe.anAnsweredQuestion
      .overlap("Shared Title")
      .withMemoryTrackerId(1)
      .please()
    const answer = mockSdkService(
      RecallPromptController,
      "answerSpelling",
      result(true)
    )
      .mockResolvedValueOnce(wrapSdkError("lost response"))
      .mockResolvedValueOnce(wrapSdkResponse(accepted))
    const wrapper = await ctx.mountPage({
      attachTo: document.body,
      global: { directives: { focus: focusDirective } },
    })
    await submit(wrapper, "Shared Title")
    await retry(wrapper).trigger("click")
    await flushPromises()
    await nextTick()
    flushCapturedAnimationFrames(frames)
    await flushPromises()
    expect(
      wrapper.find('[data-testid="spelling-overlap-feedback"]').text()
    ).toContain("Your answer matches an overlapped note")
    expect(progress(wrapper)).toContain("0/2")
    expect(
      fetchPrompt.mock.calls.map(
        ([options]) => (options as GetRecallPromptData).path.memoryTracker
      )
    ).toEqual([1, 1, 1])
    expect((input(wrapper).element as HTMLInputElement).value).toBe("")
    expect(document.activeElement).toBe(input(wrapper).element)
    await submit(wrapper, "Specific title")
    expect(
      answer.mock.calls.map(([options]) => ({
        id: (options as AnswerSpellingData).path.recallPrompt,
        text: (options as AnswerSpellingData).body.spellingAnswer,
      }))
    ).toEqual([
      { id: 101, text: "Shared Title" },
      { id: 101, text: "Shared Title" },
      { id: 102, text: "Specific title" },
    ])
    expect(progress(wrapper)).toContain("1/2")
  })

  it.each([false, true])(
    "recovers readable accidental matches under readonly=%s and opens permitted resolution actions",
    async (readonly) => {
      const reviewed = makeMe.aNoteRealm.title("Reviewed concept").please()
      reviewed.notebookRealm.readonly = readonly
      const matched = makeMe.aNoteRealm
        .title("Readable matching concept")
        .please()
      const accepted = makeMe.anAnsweredQuestion
        .withMemoryTrackerId(1)
        .withNote(reviewed.note)
        .accidentalMatch("alias", [matched.note.noteTopology])
        .please()
      mockSdkService(NoteController, "showNote", reviewed).mockImplementation(
        async (options) =>
          wrapSdkResponse(
            (options as ShowNoteData).path.note === matched.id
              ? matched
              : reviewed
          )
      )
      mockSdkService(
        RecallPromptController,
        "answerSpelling",
        accepted
      ).mockResolvedValueOnce(wrapSdkError("lost response"))
      const wrapper = await ctx.mountPage({
        global: { provide: { currentUser: ref(makeMe.aUser.please()) } },
      })
      await submit(wrapper, "alias")
      await retry(wrapper).trigger("click")
      await flushPromises()
      expect(
        wrapper.find('[data-testid="accidental-match-alert"]').text()
      ).toContain("alias")
      expect(
        wrapper.find('[data-testid="accidental-match-answer-link"]').text()
      ).toBe("alias")
      await wrapper
        .find('[data-testid="resolve-accidental-match"]')
        .trigger("click")
      await flushPromises()
      const dialog = document.querySelector("dialog")!
      expect(dialog.textContent).toContain("Readable matching concept")
      if (readonly) {
        expect(
          dialog.querySelector('[data-testid^="add-as-overlapped-note-"]')
        ).toBeNull()
        expect(
          dialog.querySelector(
            '[data-testid^="wiki-link-or-relationship-to-matched-note-"]'
          )
        ).toBeNull()
      } else {
        expect(dialog.textContent).toContain("Add as overlapped note")
        expect(dialog.textContent).toContain("Add wiki link or relationship")
      }
      expect(progress(wrapper)).toContain("1/2")
    }
  )
})
