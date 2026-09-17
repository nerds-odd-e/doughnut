import { RelationController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import makeMe from "donut-test-fixtures/makeMe"
import {
  deleteNoteButton,
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
    let resolveReduce: () => void
    const reduceHeld = new Promise<void>((r) => {
      resolveReduce = r
    })
    const { moonId, relationRealm } = qualifyingRelationRealmForDelete()
    mockSdkServiceWithImplementation(
      RelationController,
      "reduceToSourceProperty",
      async () => {
        await reduceHeld
        return makeMe.aNoteRealm.id(moonId).title("Moon").please()
      }
    )
    seedRelationRealmWithInboundReferences(relationRealm)
    const wrapper = await mountDeleteFormReady(relationRealm.note)

    ;(deleteNoteButton(wrapper).element as HTMLButtonElement).click()

    usePopups().popups.done("REDUCE")
    await flushPromises()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain(
      "Reducing to source property..."
    )

    resolveReduce!()
    await awaitDeleteSideEffects()

    expect(loadingModalMask()).toBeNull()
  })

  it("offers reduce-to-property using the current note after prop change without remount", async () => {
    const { relationId, moonId, moonNote, relationNote } =
      relationNotesForPropChangeTest()
    const reduceSpy = mockSdkService(
      RelationController,
      "reduceToSourceProperty",
      makeMe.aNoteRealm.id(moonId).title("Moon").please()
    )
    const wrapper = await mountDeleteFormWithNotePropChange(
      moonNote,
      relationNote
    )

    ;(deleteNoteButton(wrapper).element as HTMLButtonElement).click()

    const popups = usePopups().popups.peek()
    expect(popups?.length).toBe(1)
    const popup = popups?.[0]
    expect(popup?.type).toBe("options")
    if (popup?.type !== "options") throw new Error("Expected options popup")
    expect(popup.message).toBe(
      `"${relationNote.noteTopology.title}" is a relationship. What should happen?`
    )
    expect(popup.options[0]?.label).toMatch(
      /^Reduce to a property of the source/
    )
    expect(popup.options[1]?.label).toBe(
      `Trash "${relationNote.noteTopology.title}"`
    )

    usePopups().popups.done("REDUCE")
    await flushPromises()

    expect(reduceSpy).toHaveBeenCalledWith({
      path: { relationNote: relationId },
    })
  })
})
