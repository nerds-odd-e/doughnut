import {
  ConversationMessageController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { expect, vi, beforeEach, afterEach, describe, it } from "vitest"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import helper, {
  countHistoryEntriesAdded,
  mockSdkService,
  productionRouterAt,
} from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import type { Router } from "vue-router"
import { noteShowLocation } from "@/routes/noteShowLocation"
import makeMe from "donut-test-fixtures/makeMe"

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

  const mountConversation = (router?: Router) => {
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
      .withRouter(router)
      .withProps({
        conversation,
        user,
      })
      .mount()
  }

  it("routes to note show page when minimize button is clicked and subject is a note", async () => {
    const router = await productionRouterAt({ name: "root" })
    const historyEntriesAdded = countHistoryEntriesAdded(router)
    const wrapper = mountConversation(router)
    await wrapper.find("button.minimize-button").trigger("click")
    await flushPromises()

    expect(router.currentRoute.value).toMatchObject(
      noteShowLocation(note.noteTopology.id)
    )
    expect(historyEntriesAdded()).toBe(1)
  })

  it("toggles maximize state when maximize button is clicked", async () => {
    const wrapper = mountConversation()

    await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
    expect(wrapper.find(".subject-container").exists()).toBe(false)

    await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
    expect(wrapper.find(".subject-container").exists()).toBe(true)
  })
})
