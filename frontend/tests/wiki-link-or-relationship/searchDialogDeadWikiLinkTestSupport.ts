import {
  NoteController,
  SearchController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import createNoteStorage from "@/store/createNoteStorage"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkClick"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  makeNoteHit,
  renderSearchForm,
  typeInSearch,
} from "./searchDialogTestSupport"

export const deadWikiLinkPayload = {
  portablePath: "original text",
  displayText: "original text",
} as const

export async function pointDeadWikiLinkAndCaptureUpdate(args: {
  content: string
  payload: DeadWikiLinkPayload
  typeIn: string
  searchHits: ReturnType<typeof makeNoteHit>[]
  destinationRealm?: ReturnType<typeof MakeMe.aNoteRealm.please>
}) {
  const noteRealm = MakeMe.aNoteRealm.content(args.content).please()
  mockSdkService(
    SearchController,
    "searchForRelationshipTargetWithin",
    args.searchHits
  )
  if (args.destinationRealm) {
    mockSdkService(NoteController, "showNote", args.destinationRealm)
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

export async function pointPathMarkdownDeadLinkAndCaptureUpdate(args: {
  deadHref: string
  content: string
}) {
  const destinationRealm = MakeMe.aNoteRealm
    .title("Title")
    .inFolder(10, "ChosenFolder")
    .please()
  return pointDeadWikiLinkAndCaptureUpdate({
    content: args.content,
    payload: { portablePath: args.deadHref, displayText: "label" },
    typeIn: "Title",
    searchHits: [
      {
        hitKind: "NOTE" as const,
        noteSearchResult: MakeMe.aNoteSearchResult
          .title("Title")
          .id(destinationRealm.id)
          .notebookId(destinationRealm.notebookRealm.notebook.id)
          .please(),
      },
    ],
    destinationRealm,
  })
}
