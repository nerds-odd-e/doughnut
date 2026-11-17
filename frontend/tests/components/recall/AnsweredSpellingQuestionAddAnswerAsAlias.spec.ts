import {
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import type { AnsweredQuestion, NoteRealm } from "@generated/donut-backend-api"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, describe, it, expect } from "vitest"
import {
  accidentalMatchWithOneMatchedNote,
  mountAnsweredSpellingQuestion,
} from "./answeredSpellingQuestionTestSupport"

describe("AnsweredSpellingQuestion add the wrong answer as an alias", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  function wrongAnswerOn(
    reviewedContent: string,
    options: { answer?: string; readonly?: boolean } = {}
  ) {
    let reviewedBuilder = makeMe.aNoteRealm
      .title("付託する")
      .content(reviewedContent)
    if (options.readonly) reviewedBuilder = reviewedBuilder.readonly()
    const reviewedRealm = reviewedBuilder.please()
    const answeredQuestion = makeMe.anAnsweredQuestion
      .withNote(reviewedRealm.note)
      .wrongSpelling(options.answer ?? "つてを頼る")
      .please()
    mockSdkService(NoteController, "showNote", reviewedRealm)
    return { reviewedRealm, answeredQuestion }
  }

  async function mountWithSeed(
    answeredQuestion: AnsweredQuestion,
    seedRealms: NoteRealm[]
  ) {
    wrapper = mountAnsweredSpellingQuestion(answeredQuestion, { seedRealms })
    await flushPromises()
  }

  function expectNoAliasOffer() {
    expect(wrapper.text()).not.toContain("is now an alias")
    expect(
      wrapper.findAll("button").filter((b) => b.text().endsWith("as an alias"))
    ).toHaveLength(0)
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
    await mountWithSeed(answeredQuestion, [reviewedRealm])

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
    await mountWithSeed(answeredQuestion, [reviewedRealm])

    expect(wrapper.text()).toContain("`つてを頼る` is now an alias")
    expect(aliasButton()).toBeUndefined()
  })

  it("offers nothing when aliases is a scalar the answer cannot be added to", async () => {
    const { reviewedRealm, answeredQuestion } = wrongAnswerOn(
      "---\naliases: x\n---\nbody"
    )
    await mountWithSeed(answeredQuestion, [reviewedRealm])

    expectNoAliasOffer()
  })

  it("offers nothing on a note in a read-only notebook", async () => {
    const { reviewedRealm, answeredQuestion } = wrongAnswerOn("Some body", {
      readonly: true,
    })
    await mountWithSeed(answeredQuestion, [reviewedRealm])

    expect(wrapper.text()).toContain("is incorrect")
    expectNoAliasOffer()
  })

  it.each([
    "draw/a line",
    "back\\slash",
    "a:b",
    "tag#x",
    "up^x",
    "[[link",
    "link]]",
    "two\nlines",
    "   ",
  ])("offers nothing for the answer %j", async (answer) => {
    const { reviewedRealm, answeredQuestion } = wrongAnswerOn("Some body", {
      answer,
    })
    await mountWithSeed(answeredQuestion, [reviewedRealm])

    expect(wrapper.text()).toContain("is incorrect")
    expectNoAliasOffer()
  })

  it("offers nothing for a correct answer", async () => {
    const reviewedRealm = makeMe.aNoteRealm.content("Some body").please()
    const answeredQuestion = makeMe.anAnsweredQuestion
      .withNote(reviewedRealm.note)
      .answerCorrect(true)
      .please()
    await mountWithSeed(answeredQuestion, [reviewedRealm])

    expect(wrapper.text()).toContain("Correct")
    expectNoAliasOffer()
  })

  it("keeps the accidental-match result and resolve action without an alias offer", async () => {
    const { answeredQuestion, reviewedRealm } =
      accidentalMatchWithOneMatchedNote()
    await mountWithSeed(answeredQuestion, [reviewedRealm])

    expect(wrapper.text()).toContain("names another note")
    expect(
      wrapper.find('[data-testid="resolve-accidental-match"]').exists()
    ).toBe(true)
    expectNoAliasOffer()
  })
})
