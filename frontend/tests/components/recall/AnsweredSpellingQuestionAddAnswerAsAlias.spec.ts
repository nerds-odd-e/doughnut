import {
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, describe, it, expect } from "vitest"
import { mountAnsweredSpellingQuestion } from "./answeredSpellingQuestionTestSupport"

describe("AnsweredSpellingQuestion add the wrong answer as an alias", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  function wrongAnswerOn(reviewedContent: string) {
    const reviewedRealm = makeMe.aNoteRealm
      .title("付託する")
      .content(reviewedContent)
      .please()
    const answeredQuestion = makeMe.anAnsweredQuestion
      .withNote(reviewedRealm.note)
      .wrongSpelling("つてを頼る")
      .please()
    mockSdkService(NoteController, "showNote", reviewedRealm)
    return { reviewedRealm, answeredQuestion }
  }

  function aliasButton() {
    return wrapper
      .findAll("button")
      .find((b) => b.text() === "Add `つてを頼る` as an alias")
  }

  it("adds the typed answer to the reviewed note's aliases with one click", async () => {
    const { reviewedRealm, answeredQuestion } = wrongAnswerOn("Some body")
    const updated = makeMe.aNoteRealm
      .id(reviewedRealm.id)
      .title("付託する")
      .content("---\naliases:\n  - つてを頼る\n---\nSome body")
      .please()
    const updateSpy = mockSdkService(
      TextContentController,
      "updateNoteContent",
      updated
    )
    wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
      seedRealms: [reviewedRealm],
    })
    await flushPromises()

    await aliasButton()!.trigger("click")
    await flushPromises()

    expect(updateSpy).toHaveBeenCalledTimes(1)
    const callArgs = updateSpy.mock.calls[0]![0] as {
      path: { note: number }
      body: { content?: string }
    }
    expect(callArgs.path.note).toBe(reviewedRealm.id)
    expect(callArgs.body.content).toBe(
      "---\naliases:\n  - つてを頼る\n---\nSome body"
    )
    expect(wrapper.text()).toContain("`つてを頼る` is now an alias")
    expect(aliasButton()).toBeUndefined()
  })

  it("shows the done state and no action when the aliases already hold the answer", async () => {
    const { reviewedRealm, answeredQuestion } = wrongAnswerOn(
      "---\naliases:\n  - つてを頼る\n---\nSome body"
    )
    wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
      seedRealms: [reviewedRealm],
    })
    await flushPromises()

    expect(wrapper.text()).toContain("`つてを頼る` is now an alias")
    expect(aliasButton()).toBeUndefined()
  })
})
