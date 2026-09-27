import {
  ConversationMessageController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import type { Conversation } from "@generated/donut-backend-api"
import MessageCenterPage from "@/pages/MessageCenterPage.vue"
import routes from "@/routes/routes"
import { describe, it, expect, beforeEach, vi } from "vitest"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { createMemoryHistory, createRouter } from "vue-router"
import { page, server } from "vitest/browser"

describe("MessageCenterPage", () => {
  it("fetch API to be called ONCE on mount", async () => {
    const getConversationsSpy = mockSdkService(
      ConversationMessageController,
      "getConversationsOfCurrentUser",
      []
    )
    helper
      .component(MessageCenterPage)
      .withRouter()
      .withCleanStorage()
      .withProps({})
      .render()
    expect(getConversationsSpy).toBeCalledTimes(1)
  })

  it("should render no conversation selected by default", async () => {
    const conversation = makeMe.aConversationListItem.please()
    mockSdkService(
      ConversationMessageController,
      "getConversationsOfCurrentUser",
      [conversation]
    )
    helper
      .component(MessageCenterPage)
      .withRouter()
      .withCleanStorage()
      .withProps({})
      .render()
    await flushPromises()
    expect(document.body.textContent).toContain("No conversation selected")
  })

  describe("highlighting the selected conversation", () => {
    const conversations = [
      makeMe.aConversationListItem.please(),
      makeMe.aConversationListItem.please(),
    ]
    beforeEach(() => {
      mockSdkService(
        ConversationMessageController,
        "getConversationsOfCurrentUser",
        conversations
      )
      mockSdkService(
        ConversationMessageController,
        "getConversation",
        makeMe.aConversation.withId(conversations[1]!.id).please()
      )
    })

    const conversationItems = () =>
      document.querySelectorAll(
        '[data-testid="message-center-conversation-item"]'
      )

    it("should highlight the selected conversation", async () => {
      helper
        .component(MessageCenterPage)
        .withRouter()
        .withCleanStorage()
        .withProps({ conversationId: conversations[1]?.id })
        .render()
      await flushPromises()

      const items = conversationItems()
      expect(items).toHaveLength(2)
      expect(items[1]!.querySelector(".daisy-menu-active")).toBeTruthy()
      expect(items[0]!.querySelector(".daisy-menu-active")).toBeNull()
    })

    it("should navigate when conversation clicked", async () => {
      const router = createRouter({
        history: createMemoryHistory(),
        routes,
      })
      const pushSpy = vi.spyOn(router, "push")
      helper
        .component(MessageCenterPage)
        .withRouter(router)
        .withCleanStorage()
        .withProps({})
        .render()
      await flushPromises()

      const items = conversationItems()
      expect(items).toHaveLength(2)
      await (items[0] as HTMLElement).click()
      await flushPromises()

      expect(pushSpy).toHaveBeenCalledWith({
        name: "messageCenter",
        params: { conversationId: conversations[0]?.id },
      })
    })
  })

  describe("reading the selected conversation's subject", () => {
    const user = makeMe.aUser.please()

    const openConversation = async (conversation: Conversation) => {
      mockSdkService(
        ConversationMessageController,
        "getConversationsOfCurrentUser",
        [makeMe.aConversationListItem.please()]
      )
      mockSdkService(
        ConversationMessageController,
        "getConversation",
        conversation
      )
      mockSdkService(
        ConversationMessageController,
        "getConversationsAboutNote",
        []
      )
      mockSdkService(ConversationMessageController, "getConversationMessages", [
        { id: 1, message: "Is this right?", sender: user },
      ])
      mockSdkService(
        ConversationMessageController,
        "markConversationAsRead",
        []
      )
      const wrapper = helper
        .component(MessageCenterPage)
        .withRouter()
        .withCleanStorage()
        .withCurrentUser(user)
        .withProps({ conversationId: conversation.id })
        .mount({ attachTo: document.body })
      await flushPromises()
      return wrapper
    }

    const openNoteConversationAt = async (width: number, height: number) => {
      const original = server.config.browser.viewport
      onTestFinished(() => page.viewport(original.width, original.height))
      await page.viewport(width, height)
      const noteRealm = makeMe.aNoteRealm
        .title("Sedition")
        .content("Inciting rebellion.")
        .please()
      mockSdkService(NoteController, "showNote", noteRealm)
      const conversation = makeMe.aConversation
        .forANote(noteRealm.note)
        .please()
      return { conversation, wrapper: await openConversation(conversation) }
    }

    const expectReplySent = async (
      wrapper: Awaited<ReturnType<typeof openConversation>>,
      conversation: Conversation
    ) => {
      const replySpy = mockSdkService(
        ConversationMessageController,
        "replyToConversation",
        undefined
      )
      await wrapper.find("textarea").setValue("Yes, it is.")
      await wrapper.find("form.message-input-form").trigger("submit")
      await flushPromises()
      expect(replySpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { conversationId: conversation.id },
          body: "Yes, it is.",
        })
      )
    }

    const noteContext = () =>
      document.querySelector<HTMLElement>('article[aria-label="Note context"]')

    it("shows a note conversation's read-only note context beside its messages and usable composer on a wide screen", async () => {
      const { wrapper, conversation } = await openNoteConversationAt(1280, 800)

      const context = noteContext()!
      expect(context.textContent).toContain("Sedition")
      expect(context.textContent).toContain("Inciting rebellion.")
      expect(wrapper.find("#main-note-content").exists()).toBe(false)
      expect(wrapper.find('[aria-label="Read note context"]').isVisible()).toBe(
        false
      )
      const message = [...document.querySelectorAll("*")].find(
        (el) => el.children.length === 0 && el.textContent === "Is this right?"
      )!
      const contextBox = context.getBoundingClientRect()
      for (const el of [message, wrapper.find("textarea").element]) {
        const box = el.getBoundingClientRect()
        expect(contextBox.left).toBeGreaterThanOrEqual(box.right)
        expect(contextBox.top).toBeLessThan(box.bottom)
      }

      await expectReplySent(wrapper, conversation)
      wrapper.unmount()
    })

    it("reaches a note conversation's context from its header on a narrow screen and returns to reply", async () => {
      const { wrapper, conversation } = await openNoteConversationAt(390, 844)

      expect(noteContext()?.checkVisibility() ?? false).toBe(false)
      await wrapper.find('[aria-label="Read note context"]').trigger("click")
      await flushPromises()
      const drawerContext = document.querySelector<HTMLElement>(
        'dialog article[aria-label="Note context"]'
      )!
      expect(drawerContext.checkVisibility()).toBe(true)
      expect(drawerContext.textContent).toContain("Inciting rebellion.")

      document.querySelector<HTMLElement>("dialog .close-button")!.click()
      await flushPromises()
      expect(document.querySelector("dialog")).toBeNull()
      expect(wrapper.text()).toContain("Is this right?")
      await expectReplySent(wrapper, conversation)
      wrapper.unmount()
    })

    it("shows the answered question of a recall-prompt conversation", async () => {
      const answeredQuestion = makeMe.anAnsweredQuestion
        .withMcq(makeMe.anMcq.withQuestionStem("What is sedition?").please())
        .please()
      const conversation = makeMe.aConversation
        .forAnsweredQuestion(answeredQuestion)
        .please()

      const wrapper = await openConversation(conversation)

      expect(wrapper.text()).toContain("What is sedition?")
      expect(wrapper.find('article[aria-label="Note context"]').exists()).toBe(
        false
      )
      wrapper.unmount()
    })
  })
})
