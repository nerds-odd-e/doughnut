import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { waitFor } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { relationshipNoteContent } from "./relationshipNoteTestContent"
import {
  deleteNoteSpy,
  qualifyingRelationRealmForDelete,
  renderer,
  seedRelationRealmWithInboundReferences,
  setupNoteMoreOptionsDeleteFormTests,
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
    const wrapper = renderer.withProps({ note: relationRealm.note }).mount()

    await flushPromises()

    const deleteButton = wrapper.find('button[title="Delete note (d)"]')
    await deleteButton.trigger("click")
    await flushPromises()

    usePopups().popups.done("REDUCE_TO_SOURCE_PROPERTY")

    await waitFor(() => {
      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      expect(document.body.textContent).toContain(
        "Reducing to source property..."
      )
    })

    resolveDelete!()
    await flushPromises()

    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })

  it("offers reduce-to-property when deleting a qualifying relationship note", async () => {
    deleteNoteSpy.mockResolvedValue(wrapSdkResponse([]))
    const { relationRealm } = qualifyingRelationRealmForDelete()
    seedRelationRealmWithInboundReferences(relationRealm)
    const wrapper = renderer.withProps({ note: relationRealm.note }).mount()

    await flushPromises()

    const deleteButton = wrapper.find('button[title="Delete note (d)"]')
    await deleteButton.trigger("click")
    await flushPromises()

    const popups = usePopups().popups.peek()
    expect(popups?.length).toBe(1)
    const popup = popups?.[0]
    expect(popup?.type).toBe("options")
    if (popup?.type !== "options") throw new Error("Expected options popup")
    expect(popup.message).toBe(
      '"Note1.1.1" is a relationship. What should happen?'
    )
    expect(popup.options[0]?.label).toBe("Reduce to a property of the source")
    expect(popup.options[1]?.label).toBe('Delete "Note1.1.1"')

    usePopups().popups.done("REDUCE_TO_SOURCE_PROPERTY")
    await flushPromises()

    expect(deleteNoteSpy).toHaveBeenCalledWith({
      path: { note: relationRealm.id },
      body: {
        referenceHandling: "REDUCE_TO_SOURCE_PROPERTY",
        sourcePropertyKey: "a part of",
      },
    })
  })

  it("uses confirm when relationship note source does not resolve", async () => {
    deleteNoteSpy.mockResolvedValue(wrapSdkResponse([]))
    const relationRealm = makeMe.aNoteRealm
      .content(relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]"))
      .please()
    useStorageAccessor().value.refreshNoteRealm(relationRealm)
    const wrapper = renderer.withProps({ note: relationRealm.note }).mount()

    await flushPromises()

    const deleteButton = wrapper.find('button[title="Delete note (d)"]')
    await deleteButton.trigger("click")
    await flushPromises()

    const popups = usePopups().popups.peek()
    expect(popups?.[0]?.type).toBe("confirm")

    usePopups().popups.done(true)
    await flushPromises()

    expect(deleteNoteSpy).toHaveBeenCalledWith({
      path: { note: relationRealm.id },
      body: { referenceHandling: "LEAVE_DEAD_LINKS" },
    })
  })

  it("uses the current note id when note prop changes without remount", async () => {
    deleteNoteSpy.mockResolvedValue(wrapSdkResponse([]))
    const moonId = 501
    const relationId = 503
    const moonNote = {
      ...makeMe.aNote.please(),
      id: moonId,
      noteTopology: {
        ...makeMe.aNote.please().noteTopology,
        id: moonId,
        title: "Moon",
      },
    }
    const { relationRealm } = qualifyingRelationRealmForDelete({
      moonId,
      relationId,
    })
    const relationNote = {
      ...relationRealm.note,
      id: relationId,
      noteTopology: {
        ...relationRealm.note.noteTopology,
        id: relationId,
      },
    }
    useStorageAccessor().value.refreshNoteRealm({
      ...makeMe.aNoteRealm.title("Moon").please(),
      id: moonId,
    })
    useStorageAccessor().value.refreshNoteRealm(relationRealm)

    const wrapper = renderer.withProps({ note: moonNote }).mount()
    await flushPromises()
    await wrapper.setProps({ note: relationNote })
    await flushPromises()

    await wrapper.find('button[title="Delete note (d)"]').trigger("click")
    await flushPromises()

    const popups = usePopups().popups.peek()
    expect(popups?.length).toBe(1)
    expect(popups?.[0]?.type).toBe("options")
    if (popups?.[0]?.type !== "options") {
      throw new Error("Expected relationship delete options")
    }
    expect(popups[0].message).toBe(
      `"${relationNote.noteTopology.title}" is a relationship. What should happen?`
    )

    usePopups().popups.done("LEAVE_DEAD_LINKS")
    await flushPromises()

    expect(deleteNoteSpy).toHaveBeenCalledWith({
      path: { note: relationId },
      body: { referenceHandling: "LEAVE_DEAD_LINKS" },
    })
  })
})
