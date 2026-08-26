import {
  ConversationMessageController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { expect, vi, beforeEach, afterEach, describe, it } from "vitest"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import helper, { mockSdkService } from "@tests/helpers"
import { noteShowLocation } from "@/routes/noteShowLocation"
import makeMe from "donut-test-fixtures/makeMe"

const mockedPush = vi.fn()
vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      push: mockedPush,
    }),
  }
})

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.clearAllTimers()
})

describe("ConversationComponent", () => {
  const note = makeMe.aNote.please()
  const conversation = makeMe.aConversation.forANote(note).please()
  const user = makeMe.aUser.please()

  const mountConversation = () => {
    mockSdkService(
      ConversationMessageController,
      "getConversationsAboutNote",
      []
    )
    mockSdkService(ConversationMessageController, "getConversationMessages", [])
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
    return helper
      .component(ConversationComponent)
      .withCleanStorage()
      .withProps({
        conversation,
        user,
      })
      .mount()
  }

  beforeEach(() => {
    mockedPush.mockClear()
  })

  it("routes to note show page when minimize button is clicked and subject is a note", async () => {
    const wrapper = mountConversation()
    await wrapper.find("button.minimize-button").trigger("click")

    expect(mockedPush).toHaveBeenCalledWith(
      noteShowLocation(note.noteTopology.id)
    )
  })

  it("toggles maximize state when maximize button is clicked", async () => {
    const wrapper = mountConversation()

    await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
    expect(wrapper.find(".subject-container").exists()).toBe(false)

    await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
    expect(wrapper.find(".subject-container").exists()).toBe(true)
  })
})
