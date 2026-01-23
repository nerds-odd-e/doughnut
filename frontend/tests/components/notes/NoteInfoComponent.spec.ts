import NoteInfoComponent from "@/components/notes/NoteInfoComponent.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import helper, { mockSdkService, wrapSdkError } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import type { NoteInfo } from "@generated/backend"

describe("NoteInfoComponent", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    vi.clearAllMocks()
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("should display all memory trackers including skipped ones", () => {
    const noteInfo: NoteInfo = {
      memoryTrackers: [
        makeMe.aMemoryTracker
          .removedFromTracking(false)
          .repetitionCount(5)
          .please(),
        makeMe.aMemoryTracker
          .removedFromTracking(true)
          .repetitionCount(3)
          .please(),
      ],
      note: makeMe.aNoteRealm.please(),
      createdAt: "",
      noteType: undefined,
    }

    wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        noteInfo,
      })
      .withRouter()
      .mount({ attachTo: document.body })

    const rows = wrapper.findAll("tbody tr")
    expect(rows).toHaveLength(2)
  })

  it("should make skipped memory trackers clickable", async () => {
    const skippedMemoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(true)
      .please()
    skippedMemoryTracker.id = 123

    const noteInfo: NoteInfo = {
      memoryTrackers: [skippedMemoryTracker],
      note: makeMe.aNoteRealm.please(),
      createdAt: "",
      noteType: undefined,
    }

    wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        noteInfo,
      })
      .withRouter()
      .mount({ attachTo: document.body })

    await flushPromises()

    const row = wrapper.find("tbody tr")
    expect(row?.classes()).toContain("clickable-row")

    await row?.trigger("click")
    await flushPromises()

    expect(wrapper.vm.$router.currentRoute.value.name).toBe("memoryTrackerShow")
    expect(wrapper.vm.$router.currentRoute.value.params.memoryTrackerId).toBe(
      "123"
    )
  })

  it("should display memory trackers table when there are memory trackers", () => {
    const noteInfo: NoteInfo = {
      memoryTrackers: [makeMe.aMemoryTracker.please()],
      note: makeMe.aNoteRealm.please(),
      createdAt: "",
      noteType: undefined,
    }

    wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        noteInfo,
      })
      .withRouter()
      .mount({ attachTo: document.body })

    expect(wrapper.find("table").exists()).toBe(true)
    const h6Elements = wrapper.findAll("h6")
    expect(h6Elements.some((h6) => h6.text().includes("Memory Trackers"))).toBe(
      true
    )
  })

  it("should not display memory trackers table when there are no memory trackers", () => {
    const noteInfo: NoteInfo = {
      memoryTrackers: [],
      note: makeMe.aNoteRealm.please(),
      createdAt: "",
      noteType: undefined,
    }

    wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        noteInfo,
      })
      .withRouter()
      .mount({ attachTo: document.body })

    expect(wrapper.find("table").exists()).toBe(false)
  })

  describe("note type selection", () => {
    let updateNoteTypeSpy: ReturnType<typeof mockSdkService<"updateNoteType">>

    beforeEach(() => {
      updateNoteTypeSpy = mockSdkService("updateNoteType", undefined)
    })

    it("should display note type selection", () => {
      const noteInfo: NoteInfo = {
        memoryTrackers: [],
        note: makeMe.aNoteRealm.please(),
        createdAt: "",
        noteType: "concept",
      }

      wrapper = helper
        .component(NoteInfoComponent)
        .withProps({
          noteInfo,
        })
        .withRouter()
        .mount({ attachTo: document.body })

      const selection = wrapper.find('[data-test="note-type-selection-dialog"]')
      expect(selection.exists()).toBe(true)
      const select = selection.find("select")
      expect(select.exists()).toBe(true)
      expect(select.element.value).toBe("concept")
    })

    it("should save noteType when user selects a new type", async () => {
      const noteInfo: NoteInfo = {
        memoryTrackers: [],
        note: makeMe.aNoteRealm.please(),
        createdAt: "",
        noteType: undefined,
      }

      wrapper = helper
        .component(NoteInfoComponent)
        .withProps({
          noteInfo,
        })
        .withRouter()
        .mount({ attachTo: document.body })

      await flushPromises()

      const select = wrapper.find(
        '[data-test="note-type-selection-dialog"] select'
      )
      await select.setValue("source")
      await flushPromises()

      expect(updateNoteTypeSpy).toHaveBeenCalledWith({
        path: { note: noteInfo.note.id },
        body: "source",
      })
    })

    it("should emit noteTypeUpdated event on successful update", async () => {
      const noteInfo: NoteInfo = {
        memoryTrackers: [],
        note: makeMe.aNoteRealm.please(),
        createdAt: "",
        noteType: undefined,
      }

      wrapper = helper
        .component(NoteInfoComponent)
        .withProps({
          noteInfo,
        })
        .withRouter()
        .mount({ attachTo: document.body })

      await flushPromises()

      const select = wrapper.find(
        '[data-test="note-type-selection-dialog"] select'
      )
      await select.setValue("experience")
      await flushPromises()

      expect(wrapper.emitted()).toHaveProperty("noteTypeUpdated")
      expect(wrapper.emitted("noteTypeUpdated")?.[0]).toEqual(["experience"])
    })

    it("should revert selection on error", async () => {
      const noteInfo: NoteInfo = {
        memoryTrackers: [],
        note: makeMe.aNoteRealm.please(),
        createdAt: "",
        noteType: "concept",
      }

      updateNoteTypeSpy.mockResolvedValue(wrapSdkError("Failed to update"))

      wrapper = helper
        .component(NoteInfoComponent)
        .withProps({
          noteInfo,
        })
        .withRouter()
        .mount({ attachTo: document.body })

      await flushPromises()

      const select = wrapper.find(
        '[data-test="note-type-selection-dialog"] select'
      )
      await select.setValue("initiative")
      await flushPromises()

      expect(updateNoteTypeSpy).toHaveBeenCalled()
      // Should not emit noteTypeUpdated on error
      expect(wrapper.emitted("noteTypeUpdated")).toBeUndefined()
    })

    it("should not save when selected type matches current noteType", async () => {
      const noteInfo: NoteInfo = {
        memoryTrackers: [],
        note: makeMe.aNoteRealm.please(),
        createdAt: "",
        noteType: "initiative",
      }

      wrapper = helper
        .component(NoteInfoComponent)
        .withProps({
          noteInfo,
        })
        .withRouter()
        .mount({ attachTo: document.body })

      await flushPromises()

      // Select the same value that's already set
      const select = wrapper.find(
        '[data-test="note-type-selection-dialog"] select'
      )
      await select.setValue("initiative")
      await flushPromises()

      expect(updateNoteTypeSpy).not.toHaveBeenCalled()
    })
  })
})
