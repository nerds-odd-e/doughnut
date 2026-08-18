import {
  NoteController,
  SearchController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import SearchForm from "@/components/wiki-link-or-relationship/SearchForm.vue"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import createNoteStorage from "@/store/createNoteStorage"
import { afterEach, describe, expect, it, vi } from "vitest"
import { advanceSearchDebounce } from "@tests/helpers/searchDebounceTestSupport"
import {
  deadWikiLinkPayload,
  makeNoteHit,
  renderSearchForm,
  setupSearchDialogTests,
  typeInSearch,
} from "./searchDialogTestSupport"

async function pointDeadWikiLinkAndCaptureUpdate(args: {
  content: string
  payload: { targetToken: string; displayText: string }
  typeIn: string
  searchHits: ReturnType<typeof makeNoteHit>[]
  targetRealm?: ReturnType<typeof MakeMe.aNoteRealm.please>
}) {
  const noteRealm = MakeMe.aNoteRealm.content(args.content).please()
  mockSdkService(
    SearchController,
    "searchForRelationshipTargetWithin",
    args.searchHits
  )
  if (args.targetRealm) {
    mockSdkService(NoteController, "showNote", args.targetRealm)
  }
  const updateSpy = mockSdkService(
    TextContentController,
    "updateNoteContent",
    MakeMe.aNoteRealm.please()
  )
  const storageAccessor = useStorageAccessor()
  storageAccessor.value = createNoteStorage()
  storageAccessor.value.refreshNoteRealm(noteRealm)

  const searchInput = await renderSearchForm(
    { note: noteRealm.note, deadWikiLinkPayload: args.payload },
    { cleanStorage: false }
  )
  await typeInSearch(searchInput, args.typeIn)
  fireEvent.click(screen.getByText("Use this note"))
  await flushPromises()
  fireEvent.click(
    screen.getByText(
      `Point wiki link "${args.payload.displayText}" at this note`
    )
  )
  await flushPromises()
  return updateSpy
}

describe("SearchForm dead wiki link actions", () => {
  setupSearchDialogTests()

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  describe("Dead link - link to existing note", () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })

    it("prefills search with dead link display text and searches automatically", async () => {
      const note = MakeMe.aNote.please()
      const searchSpy = mockSdkService(
        SearchController,
        "searchForRelationshipTargetWithin",
        [makeNoteHit("Selected Note", note.noteTopology.id + 100)]
      )

      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note, deadWikiLinkPayload })
        .render()
      await flushPromises()

      const searchInput = screen.getByPlaceholderText("Search")
      expect(searchInput).toHaveValue("original text")
      await advanceSearchDebounce()

      expect(searchSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { note: note.id },
          body: expect.objectContaining({ searchKey: "original text" }),
        })
      )
    })

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
        expected: "[label](/ChosenFolder/Title.md)",
      },
      {
        deadHref: "/Folder/Missing",
        expected: "[label](/ChosenFolder/Title)",
      },
    ])(
      "rewrites path Markdown dead link $deadHref keeping Markdown spelling",
      async ({ deadHref, expected }) => {
        const targetRealm = MakeMe.aNoteRealm
          .title("Title")
          .inFolder(10, "ChosenFolder")
          .please()
        const updateSpy = await pointDeadWikiLinkAndCaptureUpdate({
          content: `See [label](${deadHref}) here.`,
          payload: { targetToken: deadHref, displayText: "label" },
          typeIn: "Title",
          searchHits: [
            {
              hitKind: "NOTE" as const,
              noteSearchResult: MakeMe.aNoteSearchResult
                .title("Title")
                .id(targetRealm.id)
                .notebookId(targetRealm.notebookRealm.notebook.id)
                .please(),
            },
          ],
          targetRealm,
        })

        expect(updateSpy).toHaveBeenCalledWith(
          expect.objectContaining({
            body: expect.objectContaining({
              content: `See ${expected} here.`,
            }),
          })
        )
      }
    )
  })
})
