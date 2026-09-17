import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import {
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  trashNoteButton,
  trashNoteSpy,
  loadingModalMask,
  mountTrashFormReady,
  mountTrashFormWithNotePropChange,
  qualifyingRelationRealmForTrash,
  relationNotesForPropChangeTest,
  seedRelationRealmWithInboundReferences,
  setupNoteMoreOptionsTrashFormTests,
  awaitTrashSideEffects,
} from "./noteMoreOptionsTrashTestSupport"

setupNoteMoreOptionsTrashFormTests()

describe("NoteMoreOptionsForm trash relationship note", () => {
  it("shows LoadingModal while reducing relationship note to source property", async () => {
    let resolveTrash: () => void
    const trashHeld = new Promise<void>((r) => {
      resolveTrash = r
    })
    mockSdkServiceWithImplementation(NoteController, "trashNote", async () => {
      await trashHeld
      return qualifyingRelationRealmForTrash().relationRealm
    })

    const { relationRealm } = qualifyingRelationRealmForTrash()
    seedRelationRealmWithInboundReferences(relationRealm)
    const wrapper = await mountTrashFormReady(relationRealm.note)

    ;(trashNoteButton(wrapper).element as HTMLButtonElement).click()

    usePopups().popups.done("REDUCE_TO_SOURCE_PROPERTY")
    await flushPromises()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain(
      "Reducing to source property..."
    )

    resolveTrash!()
    await awaitTrashSideEffects()

    expect(loadingModalMask()).toBeNull()
  })

  it("offers reduce-to-property using the current note after prop change without remount", async () => {
    const { relationId, moonNote, relationNote } =
      relationNotesForPropChangeTest()
    trashNoteSpy.mockResolvedValue(
      wrapSdkResponse(
        qualifyingRelationRealmForTrash({ relationId }).relationRealm
      )
    )
    const wrapper = await mountTrashFormWithNotePropChange(
      moonNote,
      relationNote
    )

    ;(trashNoteButton(wrapper).element as HTMLButtonElement).click()

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
      `Trash "${relationNote.noteTopology.title}"`
    )

    usePopups().popups.done("REDUCE_TO_SOURCE_PROPERTY")
    await flushPromises()

    expect(trashNoteSpy).toHaveBeenCalledWith({
      path: { note: relationId },
      body: {
        referenceHandling: "REDUCE_TO_SOURCE_PROPERTY",
        sourcePropertyKey: "a part of",
      },
    })
  })
})
