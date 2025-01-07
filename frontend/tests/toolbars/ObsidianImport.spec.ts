import { describe, it, expect, beforeEach } from "vitest"
import { mount, VueWrapper } from "@vue/test-utils"
import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import ObsidianImportDialog from "@/components/notes/ObsidianImportDialog.vue"
import { createRouter, createWebHistory } from "vue-router"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { Note } from "@/generated/backend"

describe("ObsidianImport", () => {
  let wrapper: VueWrapper
  let note: Note
  let storageAccessor: StorageAccessor

  beforeEach(() => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [{ path: "/", component: {} }],
    })

    note = {
      id: 1,
    } as Note

    storageAccessor = {
      deleteNote: vi.fn(),
      createNote: vi.fn(),
      updateNote: vi.fn(),
      getNote: vi.fn(),
      getNotes: vi.fn(),
      searchNotes: vi.fn(),
    } as unknown as StorageAccessor

    wrapper = mount(NoteToolbar, {
      global: {
        plugins: [router],
      },
      props: {
        note,
        storageAccessor,
      },
    })
  })

  describe("when clicking Import from Obsidian button", () => {
    beforeEach(async () => {
      // Open the dropdown menu
      await wrapper.find('button[title="more options"]').trigger("click")
    })

    it("should render Import from Obsidian button", () => {
      const button = wrapper.find('[title="Import from Obsidian"]')
      expect(button.exists()).toBe(true)
    })

    it("should open ObsidianImportDialog with correct props", async () => {
      // Click the Import from Obsidian button
      await wrapper.find('[title="Import from Obsidian"]').trigger("click")

      // Verify dialog is rendered with correct props
      const dialog = wrapper.findComponent(ObsidianImportDialog)
      expect(dialog.exists()).toBe(true)
      expect(dialog.props()).toEqual({
        note,
        storageAccessor,
      })
    })

    it("should close dialog when close-dialog event is emitted", async () => {
      // Open dialog
      await wrapper.find('[title="Import from Obsidian"]').trigger("click")
      const dialog = wrapper.findComponent(ObsidianImportDialog)

      // Emit close event
      await dialog.vm.$emit("close-dialog")

      // Wait for next tick to ensure changes are processed
      await wrapper.vm.$nextTick()

      // Verify dialog is closed
      expect(wrapper.findComponent(ObsidianImportDialog).exists()).toBe(false)
    })
  })
})
