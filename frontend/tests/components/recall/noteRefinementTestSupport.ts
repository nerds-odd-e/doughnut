import {
  AiController,
  NoteController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NoteRefinement from "@/components/recall/NoteRefinement.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import usePopups from "@/components/commons/Popups/usePopups"
import { afterEach, beforeEach, vi } from "vitest"

export const noteRealm = makeMe.aNoteRealm.please()
export const memoryTracker = makeMe.aMemoryTracker.ofNote(noteRealm).please()
export const { note } = memoryTracker

export let renderer: RenderingHelper<typeof NoteRefinement>

export function setupNoteRefinementTests() {
  beforeEach(() => {
    mockSdkService(AiController, "removePointFromNote", {
      content: "Updated content",
    })
    mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
    mockSdkService(
      AiController,
      "promotePointToSibling",
      makeMe.aNoteRealm.please()
    )
    renderer = helper.component(NoteRefinement)
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.clearAllMocks()
    const popups = usePopups()
    while (popups.popups.peek().length) {
      popups.popups.done(false)
    }
  })
}

export function mountNoteRefinement(
  points: string[],
  overrides?: { note?: typeof note }
) {
  mockSdkService(AiController, "generateUnderstandingChecklist", { points })
  return renderer
    .withCleanStorage()
    .withProps({
      note: overrides?.note ?? note,
    })
    .mount()
}

export function checklist(wrapper: {
  find: (s: string) => { findAll: (s: string) => unknown[] }
}) {
  return wrapper.find('[data-test-id="understanding-checklist"]')
}

export async function selectFirstCheckpoint(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  const checkboxes = checklist(wrapper).findAll('input[type="checkbox"]') as {
    setValue: (v: boolean) => Promise<void>
  }[]
  await checkboxes[0]?.setValue(true)
  await flushPromises()
}

export const extractToSiblingButtonTitle = "Promote to sibling note"
