import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, testFolderStub } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  renderer,
  setupNoteMoreOptionsTrashFormTests,
  awaitTrashSideEffects,
} from "./noteMoreOptionsTrashTestSupport"

setupNoteMoreOptionsTrashFormTests()

const PERMANENT_DELETE_TITLE = "Permanently delete note (d)"

const mountTrashedNote = async () => {
  const noteRealm = makeMe.aNoteRealm
    .title("Cells")
    .ancestorFolders([
      testFolderStub(1, "_trash"),
      testFolderStub(2, "Biology"),
    ])
    .please()
  const permanentlyDeleteSpy = mockSdkService(
    NoteController,
    "permanentlyDeleteNote",
    undefined
  )
  useStorageAccessor().value.refreshNoteRealm(noteRealm)
  const wrapper = renderer.withProps({ note: noteRealm.note }).mount()
  await flushPromises()
  return { noteRealm, permanentlyDeleteSpy, wrapper }
}

const clickPermanentlyDelete = async (wrapper: VueWrapper) => {
  await wrapper
    .find(`button[title="${PERMANENT_DELETE_TITLE}"]`)
    .trigger("click")
  await flushPromises()
}

describe("NoteMoreOptionsForm permanently delete a trashed note", () => {
  it("warns what is lost and sends nothing when cancelled", async () => {
    const { permanentlyDeleteSpy, wrapper } = await mountTrashedNote()

    expect(
      wrapper.find(`button[title="${PERMANENT_DELETE_TITLE}"]`).exists()
    ).toBe(true)

    await clickPermanentlyDelete(wrapper)

    const popup = usePopups().popups.peek()?.[0]
    expect(popup?.type).toBe("confirm")
    expect(popup?.message).toBe(
      'Permanently delete "Cells"? Its learning history, questions, conversations and images are deleted too, and this cannot be undone. Earlier Git history of this notebook still contains its text.'
    )

    usePopups().popups.done(false)
    await flushPromises()
    expect(permanentlyDeleteSpy).not.toHaveBeenCalled()
  })

  it("permanently deletes the note once the warning is confirmed", async () => {
    const { noteRealm, permanentlyDeleteSpy, wrapper } =
      await mountTrashedNote()

    await clickPermanentlyDelete(wrapper)
    usePopups().popups.done(true)
    await awaitTrashSideEffects()

    expect(permanentlyDeleteSpy).toHaveBeenCalledWith({
      path: { note: noteRealm.id },
    })
  })
})
