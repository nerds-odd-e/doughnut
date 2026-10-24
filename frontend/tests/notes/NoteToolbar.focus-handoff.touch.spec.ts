import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import { focusDirective } from "@/utils/focusTarget"
import { SearchController } from "@generated/donut-backend-api/sdk.gen"
import helper, { mockSdkService } from "@tests/helpers"
import { mountSoftKeyboardPrimer } from "@tests/helpers/softKeyboardPrimerTestSupport"
import {
  mockDefaultNoteRecallInfo,
  noteToolbarProps,
  resetNoteToolbarTestState,
} from "./noteToolbarTestHelpers"
import { enableAutoUnmount, flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, describe, expect, it, vi } from "vitest"
import { fireEvent } from "@testing-library/vue"

afterEach(() => {
  document.body.innerHTML = ""
  vi.restoreAllMocks()
})
enableAutoUnmount(afterEach)

describe("Note toolbar link-target search focus handoff", () => {
  it("hands focus to the search input in the opening update", async () => {
    resetNoteToolbarTestState()
    mountSoftKeyboardPrimer()
    mockDefaultNoteRecallInfo()
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
    const wrapper = helper
      .component(NoteToolbar)
      .withCleanStorage()
      .withRouter()
      .withProps(noteToolbarProps(makeMe.aNoteRealm.please()))
      .withDirective("focus", focusDirective)
      .mount({ attachTo: document.body })
    await flushPromises()
    vi.spyOn(window, "requestAnimationFrame").mockReturnValue(1)

    await fireEvent.click(
      wrapper.find('[aria-label="Wiki link or relationship"]').element
    )
    await flushPromises()

    expect(document.activeElement).toBe(
      document.querySelector('dialog input[placeholder="Search"]')
    )
  })
})
