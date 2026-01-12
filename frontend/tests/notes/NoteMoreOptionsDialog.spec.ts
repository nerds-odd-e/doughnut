import NoteMoreOptionsDialog from "@/components/notes/accessory/NoteMoreOptionsDialog.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, {
  mockSdkService,
  wrapSdkResponse,
  wrapSdkError,
} from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import type { NoteInfo } from "@generated/backend"
import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

let renderer: RenderingHelper<typeof NoteMoreOptionsDialog>
let router: ReturnType<typeof createRouter>
let getNoteInfoSpy: ReturnType<typeof mockSdkService<"getNoteInfo">>
let updateNoteTypeSpy: ReturnType<typeof mockSdkService<"updateNoteType">>
let deleteNoteSpy: ReturnType<typeof mockSdkService<"deleteNote">>

const mockNoteInfo: NoteInfo = {
  note: makeMe.aNoteRealm.please(),
  recallSetting: {
    level: 0,
    rememberSpelling: false,
    skipMemoryTracking: false,
  },
  memoryTrackers: [],
  createdAt: "",
  noteType: undefined,
}

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
})

beforeEach(() => {
  getNoteInfoSpy = mockSdkService("getNoteInfo", mockNoteInfo)
  updateNoteTypeSpy = mockSdkService("updateNoteType", undefined)
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

  describe("initialization", () => {
    it("fetches note info on mount", async () => {
      renderer.withProps({ note }).mount()

      await flushPromises()

      expect(getNoteInfoSpy).toHaveBeenCalledWith({
        path: { note: note.id },
      })
    })

    it("displays note type selector via NoteInfoComponent", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const select = wrapper.find('select[id="note-noteType"]')
      expect(select.exists()).toBe(true)
    })

    it("initializes note type from fetched noteInfo", async () => {
      const noteInfoWithType: NoteInfo = {
        ...mockNoteInfo,
        noteType: "concept",
      }
      getNoteInfoSpy.mockResolvedValue(wrapSdkResponse(noteInfoWithType))
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const select = wrapper.find('select[id="note-noteType"]')
      expect((select.element as HTMLSelectElement).value).toBe("concept")
    })

    it("defaults to empty when noteType is not set", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const select = wrapper.find('select[id="note-noteType"]')
      expect((select.element as HTMLSelectElement).value).toBe("")
    })
  })

  describe("note type update via NoteInfoComponent", () => {
    it("updates note type when user selects a new type", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const select = wrapper.find('select[id="note-noteType"]')
      await select.setValue("source")
      await flushPromises()

      expect(updateNoteTypeSpy).toHaveBeenCalledWith({
        path: { note: mockNoteInfo.note.id },
        body: "source",
      })
    })

    it("refetches note info after successful note type update", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()
      getNoteInfoSpy.mockClear()

      const select = wrapper.find('select[id="note-noteType"]')
      await select.setValue("initiative")
      await flushPromises()

      expect(getNoteInfoSpy).toHaveBeenCalledWith({
        path: { note: note.id },
      })
    })
  })

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

  describe("NoteInfoComponent display", () => {
    it("displays NoteInfoComponent when noteInfo is available", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const noteInfoComponent = wrapper.findComponent({
        name: "NoteInfoComponent",
      })
      expect(noteInfoComponent.exists()).toBe(true)
    })

    it("does not display NoteInfoComponent when noteInfo is not available", async () => {
      getNoteInfoSpy.mockResolvedValue(wrapSdkError("Not found"))
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const noteInfoComponent = wrapper.findComponent({
        name: "NoteInfoComponent",
      })
      expect(noteInfoComponent.exists()).toBe(false)
    })
  })

  describe("action buttons", () => {
    it("displays all action buttons", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      expect(
        wrapper.find('button[title="Generate Image with DALL-E"]').exists()
      ).toBe(true)
      expect(wrapper.find('button[title="Edit Note Image"]').exists()).toBe(
        true
      )
      expect(wrapper.find('button[title="Edit Note URL"]').exists()).toBe(true)
      expect(wrapper.find('button[title="Export..."]').exists()).toBe(true)
      expect(
        wrapper.find('button[title="Questions for the note"]').exists()
      ).toBe(true)
      expect(
        wrapper.find('button[title="Assimilate this note"]').exists()
      ).toBe(true)
      expect(wrapper.find('button[title="Delete note"]').exists()).toBe(true)
    })
  })

  describe("assimilate note", () => {
    it("navigates to assimilate page when assimilate button is clicked", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilateButton = wrapper.find(
        'button[title="Assimilate this note"]'
      )
      await assimilateButton.trigger("click")

      expect(router.currentRoute.value.name).toBe("assimilateSingleNote")
      expect(router.currentRoute.value.params.noteId).toBe(String(note.id))
    })

    it("emits close-dialog when assimilate button is clicked", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilateButton = wrapper.find(
        'button[title="Assimilate this note"]'
      )
      await assimilateButton.trigger("click")

      expect(wrapper.emitted()).toHaveProperty("close-dialog")
    })
  })
})
