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
    it("rewrites a missing wiki link to the backend-authored Portable path when the destination display name collides", async () => {
      mockSdkService(NoteController, "authoredPortablePath", {
        portablePath: "ChosenFolder/Selected Note",
      })
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
            content:
              "See [[ChosenFolder/Selected Note|original text]] for details.",
          }),
        })
      )
    })
  })
})
