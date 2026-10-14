import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import { useNoteStore } from "@/store/noteStore"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import {
  AiController,
  ConversationMessageController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, describe, expect, it } from "vitest"

afterEach(() => {
  teardownGlobalClientForTesting()
})

describe("Recall help refinement handoff", () => {
  it("carries only the reviewed clarification and original answer into authorized refinement", async () => {
    const learner = makeMe.aUser.please()
    const notebook = makeMe.aNotebook.creator("another-author").please()
    const noteRealm = {
      ...makeMe.aNoteRealm
        .content("Correlated observations do not establish causation.")
        .inNotebook(notebook.id)
        .please(),
      notebookRealm: { notebook, readonly: false },
    }
    const mcq = makeMe.anMcq
      .withQuestionStem("What follows from co-movement?")
      .withChoices(["Causation", "Association"])
      .correctAnswerIndex(1)
      .testedFocus("causal inference")
      .please()
    const answeredQuestion = makeMe.anAnsweredQuestion
      .withNote(noteRealm.note)
      .withMcq(mcq)
      .withChoiceIndex(0)
      .answerCorrect(false)
      .please()
    const conversation = makeMe.aConversation
      .forAnsweredQuestion(answeredQuestion)
      .please()
    mockSdkService(NoteController, "showNote", noteRealm)
    mockSdkService(ConversationMessageController, "getConversationMessages", [
      {
        id: 1,
        message:
          "A private explanation that the learner may choose to summarize.",
        createdAt: "2026-10-12T00:00:00Z",
      },
    ])
    const generate = mockSdkService(
      AiController,
      "generateRefinementSuggestions",
      { items: [] }
    )
    const renderer = helper
      .component(ConversationComponent)
      .withCleanStorage()
      .withCurrentUser(learner)
      .withRouter()
      .withProps({ conversation, user: learner })
    useNoteStore().refreshNoteRealm(noteRealm)
    renderer.render()
    await flushPromises()

    await fireEvent.click(screen.getByText("Refine note"))
    await flushPromises()
    const clarification = screen.getByLabelText(
      "Clarification to carry into refinement"
    )
    expect((clarification as HTMLTextAreaElement).value).toBe("")
    expect(generate).not.toHaveBeenCalled()
    await fireEvent.update(
      clarification,
      "Association alone does not identify a causal direction."
    )
    await fireEvent.update(
      clarification,
      "A common cause can explain both observations."
    )
    await fireEvent.click(screen.getByText("Continue to refinement"))
    await flushPromises()

    expect(generate).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { note: noteRealm.note.id },
        body: {
          stem: "What follows from co-movement?",
          choices: ["Causation", "Association"],
          correctAnswerIndex: 1,
          selectedChoiceIndex: 0,
          testedFocus: "causal inference",
          clarification: "A common cause can explain both observations.",
        },
      })
    )
    expect(screen.getByText("Refinement layout:")).toBeDefined()
  })
})

beforeEach(() => {
  mockSdkService(
    ConversationMessageController,
    "getConversationCorrections",
    []
  )
})
