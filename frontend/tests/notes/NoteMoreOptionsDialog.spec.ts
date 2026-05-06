import NoteMoreOptionsDialog from "@/components/notes/accessory/NoteMoreOptionsDialog.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

let renderer: RenderingHelper<typeof NoteMoreOptionsDialog>
let router: ReturnType<typeof createRouter>
let deleteNoteSpy: ReturnType<typeof mockSdkService<"deleteNote">>

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
})

beforeEach(() => {
  deleteNoteSpy = mockSdkService("deleteNote", undefined)
  router = createRouter({
    history: createWebHistory(),
    routes,
  })
  renderer = helper
    .component(NoteMoreOptionsDialog)
    .withRouter(router)
    .withCleanStorage()
})

describe("NoteMoreOptionsDialog", () => {
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

  describe("note accessory updated", () => {
    it("emits note-accessory-updated when accessory is updated", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const accessory = {
        id: 1,
        noteId: note.id,
        imageUrl: "https://example.com/image.jpg",
      }
      wrapper.vm.$emit("note-accessory-updated", accessory)

      expect(wrapper.emitted("note-accessory-updated")).toBeTruthy()
      expect(wrapper.emitted("note-accessory-updated")?.[0]).toEqual([
        accessory,
      ])
    })
  })

  describe("action buttons", () => {
    it("displays all action buttons", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      expect(wrapper.find('button[title="Edit Note Image"]').exists()).toBe(
        true
      )
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
