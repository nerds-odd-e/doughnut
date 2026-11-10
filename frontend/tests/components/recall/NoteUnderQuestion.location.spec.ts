import NoteUnderQuestion from "@/components/recall/NoteUnderQuestion.vue"
import AnsweredQuestionComponent from "@/components/recall/AnsweredQuestionComponent.vue"
import AnsweredSpellingQuestion from "@/components/recall/AnsweredSpellingQuestion.vue"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import MemoryTrackerPageView from "@/pages/MemoryTrackerPageView.vue"
import {
  ConversationMessageController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService, productionRouterAt } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, describe, expect, it } from "vitest"

const folders = [
  { id: 11, name: "Outer folder" },
  { id: 12, name: "Deeper folder" },
  { id: 13, name: "Unbroken".repeat(12) },
]

afterEach(() => {
  document.body.innerHTML = ""
})

function expectReadableTrail(card: Element) {
  const bounds = card.getBoundingClientRect()
  const trail = card.querySelector(".breadcrumb-wrapper")!
  expect(trail.textContent).toContain("Learning notebook")
  expect(trail.textContent).toContain(folders[2]!.name)
  const walker = document.createTreeWalker(trail, NodeFilter.SHOW_TEXT)
  while (walker.nextNode()) {
    if (!walker.currentNode.textContent?.trim()) continue
    const range = document.createRange()
    range.selectNodeContents(walker.currentNode)
    for (const rect of range.getClientRects()) {
      expect(rect.left).toBeGreaterThanOrEqual(bounds.left)
      expect(rect.right).toBeLessThanOrEqual(bounds.right + 1)
    }
  }
  expect(trail.scrollWidth).toBeLessThanOrEqual(trail.clientWidth + 1)
}

describe("Full recall location", () => {
  it.each(["multiple choice", "spelling", "tracker", "conversation"])(
    "wraps the full deep location inside a narrow %s context",
    async (context) => {
      const realm = makeMe.aNoteRealm
        .title("A long note title".repeat(4))
        .notebookName("Learning notebook")
        .ancestorFolders(folders)
        .content("Preserved preview body.")
        .please()
      mockSdkService(NoteController, "showNote", realm)
      mockSdkService(
        ConversationMessageController,
        "getConversationMessages",
        []
      )
      mockSdkService(
        ConversationMessageController,
        "getConversationCorrections",
        []
      )
      const answeredQuestion = makeMe.anAnsweredQuestion
        .withNote(realm.note)
        .please()
      const host = document.createElement("div")
      host.style.width = "280px"
      document.body.append(host)
      const wrapper =
        context === "conversation"
          ? helper
              .component(ConversationComponent)
              .withCleanStorage()
              .withRouter()
              .withProps({
                conversation: makeMe.aConversation
                  .forAnsweredQuestion(answeredQuestion)
                  .please(),
                user: makeMe.aUser.please(),
              })
              .mount({ attachTo: host })
          : context === "tracker"
            ? helper
                .component(MemoryTrackerPageView)
                .withCleanStorage()
                .withRouter()
                .withProps({
                  memoryTracker: makeMe.aMemoryTracker.ofNote(realm).please(),
                  memoryTrackerId: 1,
                  recallHistory: [],
                })
                .mount({ attachTo: host })
            : context === "spelling"
              ? helper
                  .component(AnsweredSpellingQuestion)
                  .withCleanStorage()
                  .withRouter()
                  .withProps({
                    answeredQuestion: makeMe.anAnsweredQuestion
                      .withNote(realm.note)
                      .spelling()
                      .please(),
                  })
                  .mount({ attachTo: host })
              : helper
                  .component(AnsweredQuestionComponent)
                  .withCleanStorage()
                  .withRouter()
                  .withProps({ answeredQuestion, conversationButton: false })
                  .mount({ attachTo: host })
      await flushPromises()
      const card = wrapper.get(".note-under-question").element
      expectReadableTrail(card)
      expect(
        Array.from(card.querySelectorAll(".breadcrumb-wrapper a"), (a) =>
          a.textContent?.trim()
        )
      ).toEqual([
        "Learning notebook",
        ...folders.map((f) => f.name),
        realm.note.noteTopology.title,
      ])
      if (context === "spelling") {
        const preview = card.querySelector('[aria-label="Note context"]')!
        expect(preview.textContent).toContain("Preserved preview body.")
        expect(
          card.querySelector(".breadcrumb-wrapper")!.getBoundingClientRect()
            .bottom
        ).toBeLessThanOrEqual(preview.getBoundingClientRect().top)
      }
      wrapper.unmount()
    }
  )

  it("shows notebook and title at the root and follows notebook and folder destinations", async () => {
    const realm = makeMe.aNoteRealm
      .title("Root note")
      .notebookName("Learning notebook")
      .please()
    const read = mockSdkService(NoteController, "showNote", realm)
    const router = await productionRouterAt({ name: "root" })
    const wrapper = helper
      .component(NoteUnderQuestion)
      .withCleanStorage()
      .withRealRouter(router)
      .withProps({ noteTopology: realm.note.noteTopology })
      .mount()
    await flushPromises()
    expect(
      wrapper.findAll(".breadcrumb-wrapper a").map((a) => a.text())
    ).toEqual(["Learning notebook", "Root note"])
    await wrapper.get(".breadcrumb-wrapper a").trigger("click")
    await flushPromises()
    expect(router.currentRoute.value).toMatchObject({
      name: "notebookPage",
      params: { notebookId: String(realm.notebookRealm.notebook.id) },
    })
    const nested = makeMe.aNoteRealm
      .title("Nested note")
      .notebookName("Second notebook")
      .ancestorFolders(folders)
      .please()
    read.mockResolvedValue({
      data: nested,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    await wrapper.setProps({ noteTopology: nested.note.noteTopology })
    await flushPromises()
    const folder = wrapper
      .findAll(".breadcrumb-wrapper a")
      .find((a) => a.text() === folders[1]!.name)!
    await folder.trigger("click")
    await flushPromises()
    expect(router.currentRoute.value).toMatchObject({
      name: "folderPage",
      params: {
        notebookId: String(nested.notebookRealm.notebook.id),
        folderId: "12",
      },
    })
    wrapper.unmount()
  })
})
