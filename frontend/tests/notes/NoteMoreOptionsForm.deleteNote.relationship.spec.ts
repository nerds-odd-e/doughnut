import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import {
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  clickDeleteNote,
  deleteNoteSpy,
  loadingModalMask,
  mountDeleteFormReady,
  mountDeleteFormWithNotePropChange,
  qualifyingRelationRealmForDelete,
  relationNotesForPropChangeTest,
  seedRelationRealmWithInboundReferences,
  setupNoteMoreOptionsDeleteFormTests,
  awaitDeleteSideEffects,
} from "./noteMoreOptionsDeleteTestSupport"

setupNoteMoreOptionsDeleteFormTests()

describe("NoteMoreOptionsForm delete relationship note", () => {
  it("shows LoadingModal while reducing relationship note to source property", async () => {
    let resolveDelete: () => void
    const deleteHeld = new Promise<void>((r) => {
      resolveDelete = r
    })
    mockSdkServiceWithImplementation(NoteController, "deleteNote", async () => {
      await deleteHeld
      return []
    })

    const { relationRealm } = qualifyingRelationRealmForDelete()
    seedRelationRealmWithInboundReferences(relationRealm)
    const wrapper = await mountDeleteFormReady(relationRealm.note)

    await clickDeleteNote(wrapper)

    usePopups().popups.done("REDUCE_TO_SOURCE_PROPERTY")
    await flushPromises()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain(
      "Reducing to source property..."
    )

    resolveDelete!()
    await awaitDeleteSideEffects()

    expect(loadingModalMask()).toBeNull()
  })

  it("offers reduce-to-property using the current note after prop change without remount", async () => {
    deleteNoteSpy.mockResolvedValue(wrapSdkResponse([]))
    const { relationId, moonNote, relationNote } =
      relationNotesForPropChangeTest()
    const wrapper = await mountDeleteFormWithNotePropChange(
      moonNote,
      relationNote
    )

    await clickDeleteNote(wrapper)

    const popups = usePopups().popups.peek()
    expect(popups?.length).toBe(1)
    const popup = popups?.[0]
    expect(popup?.type).toBe("options")
    if (popup?.type !== "options") throw new Error("Expected options popup")
    expect(popup.message).toBe(
      `"${relationNote.noteTopology.title}" is a relationship. What should happen?`
    )
    expect(popup.options[0]?.label).toBe("Reduce to a property of the source")
    expect(popup.options[1]?.label).toBe(
      `Delete "${relationNote.noteTopology.title}"`
    )

    usePopups().popups.done("REDUCE_TO_SOURCE_PROPERTY")
    await flushPromises()

    expect(deleteNoteSpy).toHaveBeenCalledWith({
      path: { note: relationId },
      body: {
        referenceHandling: "REDUCE_TO_SOURCE_PROPERTY",
        sourcePropertyKey: "a part of",
      },
    })
  })
})
