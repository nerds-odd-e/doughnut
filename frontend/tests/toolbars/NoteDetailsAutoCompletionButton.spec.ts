import AIClarifyingQuestionDialog from "@/components/notes/AIClarifyingQuestionDialog.vue"
import NoteDetailsAutoCompletionButton from "@/components/notes/core/NoteDetailsAutoCompletionButton.vue"
import type {
  AiAssistantResponse,
  AiCompletionParams,
  Note,
} from "@/generated/backend"
import { CancelablePromise } from "@/generated/backend"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("NoteDetailsAutoCompletionButton", () => {
  const note = makeMe.aNote.please()
  const mockedGetCompletion =
    vitest.fn<
      (
        id: number,
        params: AiCompletionParams
      ) => CancelablePromise<AiAssistantResponse>
    >()
  const mockedAnswerClarifyingQuestion = vitest.fn().mockResolvedValue({
    requiredAction: {
      contentToAppend: "auto completed content",
    },
  })
  const mockedUpldateDetails = vitest.fn()

  beforeEach(() => {
    helper.managedApi.restAiController.getCompletion = mockedGetCompletion
    helper.managedApi.restAiController.answerCompletionClarifyingQuestion =
      mockedAnswerClarifyingQuestion
    helper.managedApi.restTextContentController.updateNoteDetails =
      mockedUpldateDetails
  })

  const triggerAutoCompletionWithoutFlushPromises = async (n: Note) => {
    const wrapper = helper
      .component(NoteDetailsAutoCompletionButton)
      .withStorageProps({ note: n })
      .mount()
    await wrapper.find(".btn").trigger("click")
    return wrapper
  }

  const triggerAutoCompletion = async (n: Note) => {
    const wrapper = triggerAutoCompletionWithoutFlushPromises(n)
    await flushPromises()
    return wrapper
  }

  it("ask api to generate details when details is empty", async () => {
    const noteWithNoDetails = makeMe.aNote.details("").please()
    mockedGetCompletion.mockResolvedValue({
      requiredAction: { contentToAppend: "auto completed content" },
    })
    await triggerAutoCompletion(noteWithNoDetails)
    expect(mockedGetCompletion).toHaveBeenCalledWith(
      noteWithNoDetails.id,
      expect.objectContaining({ detailsToComplete: "" })
    )
    expect(mockedUpldateDetails).toHaveBeenCalledWith(
      noteWithNoDetails.id,
      expect.anything()
    )
  })

  it("ask api be called once when clicking the auto-complete button", async () => {
    mockedGetCompletion.mockResolvedValue({
      requiredAction: { contentToAppend: "auto completed content" },
    })
    await triggerAutoCompletion(note)
    expect(mockedGetCompletion).toHaveBeenCalledWith(
      note.id,
      expect.objectContaining({
        detailsToComplete: "<p>Desc</p>",
      })
    )
    expect(mockedUpldateDetails).toHaveBeenCalled()
  })

  it("get more completed content and update", async () => {
    mockedGetCompletion.mockResolvedValue({
      requiredAction: {
        contentToAppend: "auto completed content",
      },
    })

    await triggerAutoCompletion(note)

    expect(mockedUpldateDetails).toHaveBeenCalled()
  })

  it("ask for clarifying question", async () => {
    mockedGetCompletion.mockResolvedValue({
      runId: "run-id",
      threadId: "thread-id",
      requiredAction: {
        toolCallId: "tool-call-id",
        clarifyingQuestion: {
          question: "what do you mean?",
        },
      },
    })
    const wrapper = await triggerAutoCompletion(note)
    const dialog = wrapper.getComponent(AIClarifyingQuestionDialog)
    await dialog.find("input#note-answerToAI").setValue("I mean this")
    await dialog.find("input[type='submit']").trigger("click")
    expect(mockedAnswerClarifyingQuestion).toHaveBeenCalledWith(
      expect.objectContaining({
        toolCallId: "tool-call-id",
      })
    )
  })

  it("stop updating if the component is unmounted", async () => {
    mockedGetCompletion.mockReturnValue(
      new CancelablePromise(() => ({
        requiredAction: {
          contentToAppend: "auto completed content",
        },
      }))
    )

    const wrapper = await triggerAutoCompletionWithoutFlushPromises(note)
    wrapper.unmount()
    await flushPromises()
    // no future api call expected.
    // Because the component is unmounted.
  })
})
