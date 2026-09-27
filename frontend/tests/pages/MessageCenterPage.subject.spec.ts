import {
  ConversationMessageController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import type { Conversation, NoteRealm } from "@generated/donut-backend-api"
import MessageCenterPage from "@/pages/MessageCenterPage.vue"
import { describe, it, expect, onTestFinished } from "vitest"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { page, server } from "vitest/browser"

describe("MessageCenterPage reading the selected conversation's subject", () => {
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
    mockSdkService(ConversationMessageController, "markConversationAsRead", [])
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

  const openNoteConversationAt = async (
    width: number,
    height: number,
    noteRealm: NoteRealm = makeMe.aNoteRealm
      .title("Sedition")
      .content("Inciting rebellion.")
      .please()
  ) => {
    const original = server.config.browser.viewport
    onTestFinished(() => page.viewport(original.width, original.height))
    await page.viewport(width, height)
    mockSdkService(NoteController, "showNote", noteRealm)
    const conversation = makeMe.aConversation.forANote(noteRealm.note).please()
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

  it("keeps the close button and long location clear in a narrow note-context drawer", async () => {
    const noteRealm = makeMe.aNoteRealm
      .title("Sedition")
      .content("Inciting rebellion.")
      .notebookName("Spanish vocabulary")
      .ancestorFolders([
        { id: 1, name: "Grammar" },
        { id: 2, name: "Irregular verbs" },
      ])
      .please()
    const { wrapper, conversation } = await openNoteConversationAt(
      390,
      844,
      noteRealm
    )

    expect(noteContext()?.checkVisibility() ?? false).toBe(false)
    await wrapper.find('[aria-label="Read note context"]').trigger("click")
    await flushPromises()
    const drawer = document.querySelector<HTMLElement>("dialog .modal-right")!
    const drawerContext = drawer.querySelector<HTMLElement>(
      'article[aria-label="Note context"]'
    )!
    expect(drawerContext.checkVisibility()).toBe(true)
    expect(drawerContext.textContent).toContain("Inciting rebellion.")

    const closeButton = drawer.querySelector<HTMLElement>(".close-button")!
    const closeBox = closeButton.getBoundingClientRect()
    const hit = document.elementFromPoint(
      Math.floor(closeBox.left + closeBox.width / 2),
      Math.floor(closeBox.top + closeBox.height / 2)
    )
    expect(closeButton.contains(hit)).toBe(true)

    const location =
      drawerContext.querySelector<HTMLElement>(".daisy-breadcrumbs")!
    const drawerBox = drawer.getBoundingClientRect()
    expect(location.getBoundingClientRect().right).toBeLessThanOrEqual(
      drawerBox.right + 0.5
    )
    for (const segment of [
      "Spanish vocabulary",
      "Grammar",
      "Irregular verbs",
    ]) {
      const item = [...location.querySelectorAll("li")].find((li) =>
        li.textContent?.includes(segment)
      )!
      item.scrollIntoView({ inline: "nearest", block: "nearest" })
      const itemBox = item.getBoundingClientRect()
      const scrollport = location.getBoundingClientRect()
      expect(itemBox.left).toBeGreaterThanOrEqual(scrollport.left - 0.5)
      expect(itemBox.right).toBeLessThanOrEqual(scrollport.right + 0.5)
    }

    closeButton.click()
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
