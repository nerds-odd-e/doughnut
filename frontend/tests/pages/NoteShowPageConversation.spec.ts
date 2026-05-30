import {
  ConversationMessageController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NoteShowPageWithNotebookSidebarLayout from "@tests/fixtures/NoteShowPageWithNotebookSidebarLayout.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockNotebookGetForNoteRealm,
  mockSdkService,
} from "@tests/helpers"
import { createNoteShowPageRouter } from "@tests/pages/noteShowPageTestSupport"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"

describe("note show page conversation", () => {
  const note = makeMe.aNoteRealm.please()
  let router: ReturnType<typeof createNoteShowPageRouter>

  beforeEach(() => {
    router = createNoteShowPageRouter()
    mockSdkService(NoteController, "showNote", note)
    mockSdkService(
      ConversationMessageController,
      "getConversationsAboutNote",
      []
    )
    mockNotebookGetForNoteRealm(note)
  })

  it("should maximize conversation when maximize button is clicked", async () => {
    const wrapper = helper
      .component(NoteShowPageWithNotebookSidebarLayout)
      .withCurrentUser(makeMe.aUser.please())
      .withCleanStorage()
      .withProps({
        noteId: note.id,
      })
      .withRouter(router)
      .mount()

    await flushPromises()

    await router.push({
      name: "noteShow",
      params: {
        noteId: String(note.id),
      },
      query: { conversation: "true" },
    })
    await flushPromises()

    await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
    expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

    await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
    expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
  })

  it("should restore maximized state before closing conversation", async () => {
    const wrapper = helper
      .component(NoteShowPageWithNotebookSidebarLayout)
      .withCurrentUser(makeMe.aUser.please())
      .withCleanStorage()
      .withProps({
        noteId: note.id,
      })
      .withRouter(router)
      .mount()

    await flushPromises()

    await router.push({
      name: "noteShow",
      params: {
        noteId: String(note.id),
      },
      query: { conversation: "true" },
    })
    await flushPromises()

    await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
    expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

    await wrapper.find('[aria-label="Close dialog"]').trigger("click")
    await flushPromises()

    expect(router.currentRoute.value.name).toBe("noteShow")
    expect(router.currentRoute.value.params.noteId).toBe(String(note.id))
    expect(router.currentRoute.value.query.conversation).toBeUndefined()
    expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
    expect(wrapper.find(".conversation-container").exists()).toBe(false)
  })

  it("should open conversation when URL has conversation=true", async () => {
    router.push({
      name: "noteShow",
      params: {
        noteId: String(note.id),
      },
      query: { conversation: "true" },
    })
    await flushPromises()

    const wrapper = helper
      .component(NoteShowPageWithNotebookSidebarLayout)
      .withCurrentUser(makeMe.aUser.please())
      .withCleanStorage()
      .withProps({
        noteId: note.id,
      })
      .withRouter(router)
      .mount()

    await flushPromises()

    expect(wrapper.find(".conversation-wrapper").exists()).toBe(true)
  })
})
