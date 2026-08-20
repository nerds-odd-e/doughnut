import { describe, expect, it } from "vitest"
import MakeMe from "doughnut-test-fixtures/makeMe"
import {
  deadWikiLinkPayload,
  pointDeadWikiLinkAndCaptureUpdate,
  pointPathMarkdownDeadLinkAndCaptureUpdate,
} from "./searchDialogDeadWikiLinkTestSupport"
import {
  makeNoteHit,
  setupSearchDialogFakeTimers,
  setupSearchDialogTests,
} from "./searchDialogTestSupport"

describe("SearchForm dead wiki link actions", () => {
  setupSearchDialogTests()
  setupSearchDialogFakeTimers()

  describe("Dead link - link to existing note", () => {
    it("rewrites note content when linking dead link to existing note", async () => {
      const note = MakeMe.aNote.please()
      const updateSpy = await pointDeadWikiLinkAndCaptureUpdate({
        content: "See [[original text]] for details.",
        payload: deadWikiLinkPayload,
        typeIn: "Selected",
        searchHits: [makeNoteHit("Selected Note", note.noteTopology.id + 100)],
      })

      expect(updateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          body: expect.objectContaining({
            content: "See [[Selected Note|original text]] for details.",
          }),
        })
      )
    })

    it.each([
      {
        deadHref: "/Folder/Missing.md",
        content: "[label](/Folder/Missing.md) [label](/Folder/Missing.md)",
        expected:
          "[label](/ChosenFolder/Title.md) [label](/ChosenFolder/Title.md)",
      },
      {
        deadHref: "/Folder/Missing",
        content: "See [label](/Folder/Missing) here.",
        expected: "See [label](/ChosenFolder/Title) here.",
      },
    ])(
      "rewrites path Markdown dead link $deadHref keeping Markdown spelling",
      async ({ deadHref, content, expected }) => {
        const updateSpy = await pointPathMarkdownDeadLinkAndCaptureUpdate({
          deadHref,
          content,
        })

        expect(updateSpy).toHaveBeenCalledWith(
          expect.objectContaining({
            body: expect.objectContaining({ content: expected }),
          })
        )
      }
    )
  })
})
