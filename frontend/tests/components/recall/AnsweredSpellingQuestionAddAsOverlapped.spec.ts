import {
  NoteController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import { buildWikiLinkText } from "@/utils/buildWikiLinkText"
import makeMe from "doughnut-test-fixtures/makeMe"
import { afterEach, beforeEach, describe, it, expect } from "vitest"
import {
  accidentalMatchWithTwoMatchedNotes,
  mountAnsweredSpellingQuestion,
  openResolveAccidentalMatch,
} from "./answeredSpellingQuestionTestSupport"

describe("AnsweredSpellingQuestion add as overlapped note", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("adds as overlapped note via wiki-link content update without try-again", async () => {
    const { answeredQuestion, reviewedRealm, matchedA, matchedB } =
      accidentalMatchWithTwoMatchedNotes()
    const updateSpy = mockSdkService(
      TextContentController,
      "updateNoteContent",
      reviewedRealm
    )

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
      currentUser: makeMe.aUser.please(),
      seedRealms: [reviewedRealm, matchedA, matchedB],
      withRouter: true,
    })
    await flushPromises()
    await openResolveAccidentalMatch(wrapper)

    const overlapButtons = [
      ...document.body.querySelectorAll(
        '[data-testid^="add-as-overlapped-note-"]'
      ),
    ]
    expect(overlapButtons).toHaveLength(2)
    expect(
      overlapButtons.every((btn) =>
        btn.textContent?.includes("Add as overlapped note")
      )
    ).toBe(true)
    expect(
      document.body.querySelectorAll('[data-testid^="link-to-matched-note-"]')
    ).toHaveLength(2)

    updateSpy.mockClear()
    ;(
      document.body.querySelector(
        '[data-testid="add-as-overlapped-note-10"]'
      ) as HTMLElement
    ).click()
    await flushPromises()

    expect(updateSpy).toHaveBeenCalledTimes(1)
    const callArgs = updateSpy.mock.calls[0]![0] as {
      path: { note: number }
      body: { content?: string }
    }
    expect(callArgs.path.note).toBe(reviewedRealm.id)
    expect(callArgs.body.content).toMatch(/overlaps:/)
    expect(callArgs.body.content).toContain("[[")
    expect(callArgs.body.content).not.toMatch(/aliases:/)
    expect(
      document.body.querySelector(
        '[data-testid="accidental-match-resolve-dialog"]'
      )
    ).toBeTruthy()
    expect(
      wrapper.find('[data-testid="accidental-match-alert"]').exists()
    ).toBe(true)
    expect(wrapper.find('[data-testid="overlap-try-again"]').exists()).toBe(
      false
    )
    expect(
      wrapper.find('[data-testid="overlap-try-again-alert"]').exists()
    ).toBe(false)
    expect(wrapper.emitted("retry")).toBeUndefined()
  })

  it("disables add as overlapped when the match is already in overlaps", async () => {
    const { answeredQuestion, reviewedRealm, matchedA, matchedB } =
      accidentalMatchWithTwoMatchedNotes()
    const overlapToken = buildWikiLinkText(
      {
        noteTopology: matchedA.note.noteTopology,
        notebookId: matchedA.notebookRealm.notebook.id,
        notebookName: matchedA.notebookRealm.notebook.name,
      },
      { notebookId: reviewedRealm.notebookRealm.notebook.id }
    )
    reviewedRealm.note.content = `---
overlaps:
  - "${overlapToken}"
---

## Body
`

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
      currentUser: makeMe.aUser.please(),
      seedRealms: [reviewedRealm, matchedA, matchedB],
      withRouter: true,
    })
    await flushPromises()
    await openResolveAccidentalMatch(wrapper)

    const alreadyDeclared = document.body.querySelector(
      '[data-testid="add-as-overlapped-note-10"]'
    ) as HTMLButtonElement
    const otherMatch = document.body.querySelector(
      '[data-testid="add-as-overlapped-note-20"]'
    ) as HTMLButtonElement
    expect(alreadyDeclared.disabled).toBe(true)
    expect(otherMatch.disabled).toBe(false)
    expect(
      document.body.querySelector(
        '[data-testid="link-to-matched-note-10"]'
      ) as HTMLButtonElement
    ).not.toBeNull()
    expect(
      (
        document.body.querySelector(
          '[data-testid="link-to-matched-note-10"]'
        ) as HTMLButtonElement
      ).disabled
    ).toBe(false)
  })

  it("disables add as overlapped when the match is a legacy wiki-link in aliases", async () => {
    const { answeredQuestion, reviewedRealm, matchedA, matchedB } =
      accidentalMatchWithTwoMatchedNotes()
    const overlapToken = buildWikiLinkText(
      {
        noteTopology: matchedA.note.noteTopology,
        notebookId: matchedA.notebookRealm.notebook.id,
        notebookName: matchedA.notebookRealm.notebook.name,
      },
      { notebookId: reviewedRealm.notebookRealm.notebook.id }
    )
    reviewedRealm.note.content = `---
aliases:
  - "${overlapToken}"
---

## Body
`

    wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
      currentUser: makeMe.aUser.please(),
      seedRealms: [reviewedRealm, matchedA, matchedB],
      withRouter: true,
    })
    await flushPromises()
    await openResolveAccidentalMatch(wrapper)

    expect(
      (
        document.body.querySelector(
          '[data-testid="add-as-overlapped-note-10"]'
        ) as HTMLButtonElement
      ).disabled
    ).toBe(true)
  })
})
