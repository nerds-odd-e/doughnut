import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteMoreOptionsForm from "@/components/notes/widgets/NoteMoreOptionsForm.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

let renderer: RenderingHelper<typeof NoteMoreOptionsForm>
let router: ReturnType<typeof createRouter>
let deleteNoteSpy: ReturnType<typeof mockSdkService>

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
})

beforeEach(() => {
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

describe("NoteMoreOptionsForm", () => {
  const note = makeMe.aNote.please()

  describe("close dialog", () => {
    it("emits close-dialog when close button is clicked", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const closeButton = wrapper.find('button[title="Close"]')
      await closeButton.trigger("click")

      expect(wrapper.emitted()).toHaveProperty("close-dialog")
    })
  })

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
      expect(popups?.[0]?.message).toBe("Confirm to delete this note?")

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

  describe("action buttons", () => {
    it("displays all action buttons", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      expect(wrapper.find('button[title="Export..."]').exists()).toBe(true)
      expect(
        wrapper.find('button[title="Questions for the note"]').exists()
      ).toBe(true)
      expect(
        wrapper.find('button[title="Assimilation settings"]').exists()
      ).toBe(true)
      expect(wrapper.find('button[title="Delete note"]').exists()).toBe(true)
    })
  })

  describe("assimilate note", () => {
    it("navigates to assimilate page when assimilation settings button is clicked", async () => {
      await router.push("/")
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilateButton = wrapper.find(
        'button[title="Assimilation settings"]'
      )
      await assimilateButton.trigger("click")

      await flushPromises()

      expect(router.currentRoute.value.name).toBe("assimilateSingleNote")
      expect(router.currentRoute.value.params.noteId).toBe(String(note.id))
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
  })
})
