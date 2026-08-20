import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { wrapSdkResponse } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  noteMoreOptionsDeleteFormNote as note,
  clickDeleteNote,
  deleteNoteSpy,
  renderer,
  setupNoteMoreOptionsDeleteFormTests,
  awaitDeleteSideEffects,
} from "./noteMoreOptionsDeleteTestSupport"

setupNoteMoreOptionsDeleteFormTests()

describe("NoteMoreOptionsForm delete note", () => {
  it("calls deleteNote when confirmed and skips when cancelled", async () => {
    deleteNoteSpy.mockResolvedValue(wrapSdkResponse([]))
    const wrapper = renderer.withProps({ note }).mount()

    await flushPromises()
    await clickDeleteNote(wrapper)

    expect(usePopups().popups.peek()?.[0]?.type).toBe("confirm")
    usePopups().popups.done(false)
    await flushPromises()
    expect(deleteNoteSpy).not.toHaveBeenCalled()

    await clickDeleteNote(wrapper)
    const popups = usePopups().popups.peek()
    expect(popups?.length).toBe(1)
    expect(popups?.[0]?.message).toBe('Confirm to delete "Note1.1.1"?')

    usePopups().popups.done(true)
    await awaitDeleteSideEffects()

    expect(deleteNoteSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      body: { referenceHandling: "LEAVE_DEAD_LINKS" },
    })
  })

  it("asks how to handle references when the note has inbound references", async () => {
    deleteNoteSpy.mockResolvedValue(wrapSdkResponse([]))
    const noteRealm = makeMe.aNoteRealm.please()
    useStorageAccessor().value.refreshNoteRealm({
      ...noteRealm,
      references: [makeMe.aNoteRealm.please().note.noteTopology],
    })
    const wrapper = renderer.withProps({ note: noteRealm.note }).mount()

    await flushPromises()
    await clickDeleteNote(wrapper)

    const popups = usePopups().popups.peek()
    expect(popups?.length).toBe(1)
    const popup = popups?.[0]
    expect(popup?.type).toBe("options")
    if (popup?.type !== "options") throw new Error("Expected options popup")
    expect(popup.options[0]?.label).toContain(
      "undo will not recover the removed property"
    )
    expect(popup.options[1]?.label).toBe(
      "Leave all references as dead wiki links"
    )

    usePopups().popups.done("REMOVE_FROM_PROPERTIES")
    await awaitDeleteSideEffects()

    expect(deleteNoteSpy).toHaveBeenCalledWith({
      path: { note: noteRealm.id },
      body: { referenceHandling: "REMOVE_FROM_PROPERTIES" },
    })
  })
})
