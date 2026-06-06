import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { wrapSdkResponse } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  noteMoreOptionsDeleteFormNote as note,
  deleteNoteSpy,
  renderer,
  setupNoteMoreOptionsDeleteFormTests,
} from "./noteMoreOptionsDeleteTestSupport"

setupNoteMoreOptionsDeleteFormTests()

describe("NoteMoreOptionsForm delete note", () => {
  it("calls deleteNote API when delete button is clicked and confirmed", async () => {
    deleteNoteSpy.mockResolvedValue(wrapSdkResponse([]))
    const wrapper = renderer.withProps({ note }).mount()

    await flushPromises()

    const deleteButton = wrapper.find('button[title="Delete note"]')
    await deleteButton.trigger("click")
    await flushPromises()

    const popups = usePopups().popups.peek()
    expect(popups?.length).toBe(1)
    expect(popups?.[0]?.type).toBe("confirm")
    expect(popups?.[0]?.message).toBe('Confirm to delete "Note1.1.1"?')

    usePopups().popups.done(true)
    await flushPromises()

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

    const deleteButton = wrapper.find('button[title="Delete note"]')
    await deleteButton.trigger("click")
    await flushPromises()

    const popups = usePopups().popups.peek()
    expect(popups?.length).toBe(1)
    const popup = popups?.[0]
    expect(popup?.type).toBe("options")
    if (popup?.type !== "options") throw new Error("Expected options popup")
    expect(popup.options[0]?.label).toContain(
      "undo will not recover the removed property"
    )

    usePopups().popups.done("REMOVE_FROM_PROPERTIES")
    await flushPromises()

    expect(deleteNoteSpy).toHaveBeenCalledWith({
      path: { note: noteRealm.id },
      body: { referenceHandling: "REMOVE_FROM_PROPERTIES" },
    })
  })

  it("does not call deleteNote when confirmation is cancelled", async () => {
    const wrapper = renderer.withProps({ note }).mount()

    await flushPromises()

    const storageAccessor = useStorageAccessor()
    const deleteNoteMock = vi.fn()
    const storedApi = storageAccessor.value.storedApi()
    storedApi.deleteNote = deleteNoteMock

    const deleteButton = wrapper.find('button[title="Delete note"]')
    await deleteButton.trigger("click")
    await flushPromises()

    const popups = usePopups().popups.peek()
    expect(popups?.length).toBe(1)

    usePopups().popups.done(false)
    await flushPromises()

    expect(deleteNoteMock).not.toHaveBeenCalled()
  })
})
