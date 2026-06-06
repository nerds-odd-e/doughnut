import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteMoreOptionsForm from "@/components/notes/widgets/NoteMoreOptionsForm.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import {
  resetAssimilationViewForTests,
  useAssimilationView,
} from "@/composables/useAssimilationView"
import { wikiTitleFromInnerAndNoteId } from "@/utils/wikiPropertyValueField"
import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import { setupGlobalClient } from "@/managedApi/clientSetup"

const mockToast = {
  error: vi.fn(),
  warning: vi.fn(),
}

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

let renderer: RenderingHelper<typeof NoteMoreOptionsForm>
let router: ReturnType<typeof createRouter>
let deleteNoteSpy: ReturnType<typeof mockSdkService>
const apiStatus: ApiStatus = { states: [] }

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
})

beforeEach(() => {
  resetAssimilationViewForTests()
  usePopups().popups.register({ popupInfo: [] })
  setupGlobalClient(apiStatus)
  mockToast.error.mockClear()
  mockToast.warning.mockClear()
  deleteNoteSpy = mockSdkService(NoteController, "deleteNote", undefined)
  router = createRouter({
    history: createWebHistory(),
    routes,
  })
  renderer = helper
    .component(NoteMoreOptionsForm)
    .withRouter(router)
    .withCleanStorage()
})

function relationshipNoteContent(
  relationKebab: string,
  sourceLink: string,
  targetLink: string
): string {
  return `---
type: relationship
relation: ${relationKebab}
source: "${sourceLink.replace(/"/g, '\\"')}"
target: "${targetLink.replace(/"/g, '\\"')}"
---
`
}

describe("NoteMoreOptionsForm", () => {
  const note = makeMe.aNote.please()

  describe("delete note", () => {
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

    it("offers reduce-to-property when deleting a qualifying relationship note", async () => {
      deleteNoteSpy.mockResolvedValue(wrapSdkResponse([]))
      const moonId = 501
      const earthId = 502
      const relationRealm = {
        ...makeMe.aNoteRealm
          .content(
            relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]")
          )
          .please(),
        wikiTitles: [
          wikiTitleFromInnerAndNoteId("Moon", moonId),
          wikiTitleFromInnerAndNoteId("Earth", earthId),
        ],
      }
      useStorageAccessor().value.refreshNoteRealm({
        ...relationRealm,
        references: [makeMe.aNoteRealm.please().note.noteTopology],
      })
      const wrapper = renderer.withProps({ note: relationRealm.note }).mount()

      await flushPromises()

      const deleteButton = wrapper.find('button[title="Delete note"]')
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

    it("shows conflict message when reduce-to-property delete returns 409", async () => {
      const conflictMessage =
        'The source note already has a property named "a part of".'
      deleteNoteSpy.mockResolvedValue({
        ...wrapSdkError({
          message: conflictMessage,
          errorType: "RESOURCE_CONFLICT",
        }),
        response: { status: 409 } as Response,
      })
      const moonId = 501
      const earthId = 502
      const relationRealm = {
        ...makeMe.aNoteRealm
          .content(
            relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]")
          )
          .please(),
        wikiTitles: [
          wikiTitleFromInnerAndNoteId("Moon", moonId),
          wikiTitleFromInnerAndNoteId("Earth", earthId),
        ],
      }
      useStorageAccessor().value.refreshNoteRealm({
        ...relationRealm,
        references: [makeMe.aNoteRealm.please().note.noteTopology],
      })
      const wrapper = renderer.withProps({ note: relationRealm.note }).mount()

      await flushPromises()

      const deleteButton = wrapper.find('button[title="Delete note"]')
      await deleteButton.trigger("click")
      await flushPromises()

      usePopups().popups.done("REDUCE_TO_SOURCE_PROPERTY")
      await flushPromises()

      expect(mockToast.error).toHaveBeenCalledWith(
        conflictMessage,
        expect.objectContaining({ timeout: 3000 })
      )
      expect(
        useStorageAccessor().value.refOfNoteRealm(relationRealm.id).value
      ).toBeDefined()
    })

    it("uses confirm when relationship note source does not resolve", async () => {
      deleteNoteSpy.mockResolvedValue(wrapSdkResponse([]))
      const relationRealm = makeMe.aNoteRealm
        .content(relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]"))
        .please()
      useStorageAccessor().value.refreshNoteRealm(relationRealm)
      const wrapper = renderer.withProps({ note: relationRealm.note }).mount()

      await flushPromises()

      const deleteButton = wrapper.find('button[title="Delete note"]')
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
      const relationRealm = {
        ...makeMe.aNoteRealm
          .content(
            relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]")
          )
          .please(),
        id: relationId,
        wikiTitles: [
          wikiTitleFromInnerAndNoteId("Moon", moonId),
          wikiTitleFromInnerAndNoteId("Earth", 502),
        ],
      }
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

      await wrapper.find('button[title="Delete note"]').trigger("click")
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

  describe("action buttons", () => {
    it("displays all action buttons", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      expect(wrapper.find('button[title="Export..."]').exists()).toBe(true)
      expect(wrapper.get("ul").text()).toContain("Export...")
      expect(
        wrapper.find('button[title="Questions for the note"]').exists()
      ).toBe(true)
      expect(wrapper.get("ul").text()).toContain("Questions for the note")
      expect(
        wrapper.find('button[title="Assimilation settings"]').exists()
      ).toBe(true)
      expect(wrapper.get("ul").text()).toContain("Assimilation settings")
      expect(wrapper.find('button[title="Delete note"]').exists()).toBe(true)
      expect(wrapper.get("ul").text()).toContain("Delete note")
    })
  })

  describe("assimilation settings toggle", () => {
    it("turns assimilation settings on and shows check without changing route", async () => {
      await router.push("/")
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilateButton = wrapper.find(
        'button[title="Assimilation settings"]'
      )
      await assimilateButton.trigger("click")

      await flushPromises()

      expect(router.currentRoute.value.path).toBe("/")
      const { showAssimilationSettings, pendingOnForNoteId } =
        useAssimilationView()
      expect(showAssimilationSettings.value).toBe(true)
      expect(pendingOnForNoteId.value).toBe(note.id)
    })

    it("emits close-dialog when assimilation settings button is clicked", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilateButton = wrapper.find(
        'button[title="Assimilation settings"]'
      )
      await assimilateButton.trigger("click")

      expect(wrapper.emitted()).toHaveProperty("close-dialog")
    })

    it("turns assimilation settings off when toggled while already on", async () => {
      const { requestOnFor } = useAssimilationView()
      requestOnFor(note.id)

      const wrapper = renderer.withProps({ note }).mount()
      await flushPromises()

      const assimilateButton = wrapper.find(
        'button[title="Assimilation settings"]'
      )
      await assimilateButton.trigger("click")
      await flushPromises()

      const { showAssimilationSettings, pendingOnForNoteId } =
        useAssimilationView()
      expect(showAssimilationSettings.value).toBe(false)
      expect(pendingOnForNoteId.value).toBe(null)
    })
  })
})
