import { describe, expect, it } from "vitest"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import MakeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  deadWikiLinkPayload,
  pointDeadWikiLinkAndCaptureUpdate,
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

    it("rewrites an ambiguous wiki link to the backend-authored Portable path", async () => {
      mockSdkService(NoteController, "authoredPortablePath", {
        portablePath: "ChosenFolder/Selected Note",
      })
      const note = MakeMe.aNote.please()
      const updateSpy = await pointDeadWikiLinkAndCaptureUpdate({
        content: "See [[original text]] for details.",
        payload: {
          portablePath: "original text",
          displayText: "original text",
          resolution: "AMBIGUOUS",
        },
        typeIn: "Selected",
        searchHits: [makeNoteHit("Selected Note", note.noteTopology.id + 100)],
      })

      expect(updateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          body: expect.objectContaining({
            content:
              "See [[ChosenFolder/Selected Note|original text]] for details.",
          }),
        })
      )
    })
  })
})
