import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, beforeEach, describe, it, expect } from "vitest"
import {
  accidentalMatchWithTwoMatchedNotes,
  mountAnsweredSpellingQuestion,
  openResolveAccidentalMatch,
} from "./answeredSpellingQuestionTestSupport"

describe("AnsweredSpellingQuestion resolve overlap explanation", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("explains overlap meaning when resolve dialog list is shown", async () => {
    const { answeredQuestion } = accidentalMatchWithTwoMatchedNotes()
    wrapper = mountAnsweredSpellingQuestion(answeredQuestion)
    await flushPromises()
    await openResolveAccidentalMatch(wrapper)

    const explanation = document.body.querySelector(
      '[data-testid="resolve-overlap-explanation"]'
    )
    expect(explanation).toBeTruthy()
    expect(explanation?.textContent).toContain(
      "largely overlaps with the current note"
    )
    expect(explanation?.textContent).toContain("more precise answer")
  })
})
