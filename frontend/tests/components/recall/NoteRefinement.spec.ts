import NoteRefinement from "@/components/recall/NoteRefinement.vue"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, {
  mockShowNoteAccessory,
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import usePopups from "@/components/commons/Popups/usePopups"
import type { PromotePointRequestDto } from "@generated/backend"

const PromotionType = {
  CHILD: "CHILD",
  SIBLING: "SIBLING",
} as const satisfies Record<string, PromotePointRequestDto["promotionType"]>

let renderer: RenderingHelper<typeof NoteRefinement>

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
  const popups = usePopups()
  while (popups.popups.peek().length) {
    popups.popups.done(false)
  }
})

beforeEach(() => {
  mockSdkService("removePointFromNote", { details: "Updated content" })
  mockSdkService("updateNoteDetails", makeMe.aNoteRealm.please())
  mockSdkService("promotePoint", {
    createdNote: makeMe.aNoteRealm.please(),
    updatedParentNote: makeMe.aNoteRealm.please(),
  })
  mockShowNoteAccessory()
  renderer = helper.component(NoteRefinement)
})

describe("NoteRefinement component", () => {
  const noteRealm = makeMe.aNoteRealm.please()
  const memoryTracker = makeMe.aMemoryTracker.ofNote(noteRealm).please()
  const { note } = memoryTracker

  const mount = (points: string[], overrides?: { note?: typeof note }) => {
    mockSdkService("generateUnderstandingChecklist", { points })
    return renderer
      .withCleanStorage()
      .withProps({
        note: overrides?.note ?? note,
        currentNoteDetails: "Some note content",
      })
      .mount()
  }

  const checklist = (wrapper: {
    find: (s: string) => { findAll: (s: string) => unknown[] }
  }) => wrapper.find('[data-test-id="understanding-checklist"]')

  const selectFirstCheckpoint = async (wrapper: ReturnType<typeof mount>) => {
    const checkboxes = checklist(wrapper).findAll('input[type="checkbox"]') as {
      setValue: (v: boolean) => Promise<void>
    }[]
    await checkboxes[0]?.setValue(true)
    await flushPromises()
  }

  describe("promote point to child note", () => {
    it("displays promote button for each understanding point", async () => {
      const wrapper = mount(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      const listItems = wrapper.findAll("li")
      expect(listItems).toHaveLength(3)
      listItems.forEach((li, index) => {
        expect(li.text()).toContain(["Point 1", "Point 2", "Point 3"][index])
        expect(li.find("button").exists()).toBe(true)
      })
    })

    it("calls promotePoint API when child button is clicked", async () => {
      const promotePointSpy = mockSdkService("promotePoint", {
        createdNote: makeMe.aNoteRealm.please(),
        updatedParentNote: makeMe.aNoteRealm.please(),
      })
      const wrapper = mount(["Test Point"])
      await flushPromises()

      await wrapper.find("li button").trigger("click")
      await flushPromises()

      expect(promotePointSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { point: "Test Point", promotionType: PromotionType.CHILD },
      })
    })

    it("removes point from checklist after successful promotion", async () => {
      mockSdkService("promotePoint", {
        createdNote: makeMe.aNoteRealm.please(),
        updatedParentNote: makeMe.aNoteRealm.please(),
      })
      const wrapper = mount(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      await wrapper.findAll("li")[1]!.find("button").trigger("click")
      await flushPromises()

      expect(wrapper.findAll("li")).toHaveLength(2)
      expect(wrapper.text()).toContain("Point 1")
      expect(wrapper.text()).toContain("Point 3")
      expect(wrapper.text()).not.toContain("Point 2")
    })

    it("keeps point in checklist when API fails", async () => {
      mockSdkService("promotePoint", undefined).mockResolvedValue(
        wrapSdkError("API Error")
      )
      const wrapper = mount(["Test Point"])
      await flushPromises()

      await wrapper.find("li button").trigger("click")
      await flushPromises()

      expect(wrapper.findAll("li")).toHaveLength(1)
      expect(wrapper.text()).toContain("Test Point")
    })

    it("calls promotePoint with SIBLING when sibling button is clicked", async () => {
      const childNoteRealm = makeMe.aNoteRealm
        .under(makeMe.aNoteRealm.please())
        .please()
      const childNote = makeMe.aMemoryTracker
        .ofNote(childNoteRealm)
        .please().note
      const promotePointSpy = mockSdkService("promotePoint", {
        createdNote: makeMe.aNoteRealm.please(),
        updatedParentNote: makeMe.aNoteRealm.please(),
      })
      const wrapper = mount(["Test Point"], { note: childNote })
      await flushPromises()

      await wrapper.findAll("li button")[1]!.trigger("click")
      await flushPromises()

      expect(promotePointSpy).toHaveBeenCalledWith({
        path: { note: childNote.id },
        body: { point: "Test Point", promotionType: PromotionType.SIBLING },
      })
    })

    it("hides sibling button when note has no parent", async () => {
      const wrapper = mount(["Test Point"])
      await flushPromises()

      expect(
        wrapper.find('button[title="Promote to sibling note"]').exists()
      ).toBe(false)
      expect(
        wrapper.find('button[title="Promote to child note"]').exists()
      ).toBe(true)
    })
  })

  describe("understanding checklist deletion", () => {
    it("shows checkboxes for each understanding point", async () => {
      const wrapper = mount(["Point 1", "Point 2", "Point 3"])
      await flushPromises()
      expect(checklist(wrapper).findAll('input[type="checkbox"]')).toHaveLength(
        3
      )
    })

    it("disables delete button when no points are selected", async () => {
      const wrapper = mount(["Point 1", "Point 2"])
      await flushPromises()
      expect(
        (
          wrapper.find('[data-test-id="delete-understanding-points"]')
            .element as HTMLButtonElement
        ).disabled
      ).toBe(true)
    })

    it("enables delete button when a point is selected", async () => {
      const wrapper = mount(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      expect(
        (
          wrapper.find('[data-test-id="delete-understanding-points"]')
            .element as HTMLButtonElement
        ).disabled
      ).toBe(false)
    })

    it("shows confirmation dialog when delete button is clicked", async () => {
      const wrapper = mount(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups).toHaveLength(1)
      expect(popups[0]!.type).toBe("confirm")
      expect(popups[0]!.message).toContain("delete")
    })

    it("calls API and emits detailsUpdated when deletion is confirmed", async () => {
      const deletePointsSpy = mockSdkService("removePointFromNote", {
        details: "Updated content",
      })
      const updateDetailsSpy = mockSdkService(
        "updateNoteDetails",
        makeMe.aNoteRealm.please()
      )
      const wrapper = mount(["Point 1", "Point 2", "Point 3"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()

      expect(deletePointsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { points: ["Point 1"] },
      })
      expect(updateDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { details: "Updated content" },
      })
      expect(wrapper.emitted()).toHaveProperty("detailsUpdated")
      expect(wrapper.emitted("detailsUpdated")).toEqual([["Updated content"]])
    })

    it("does not call API when deletion is cancelled", async () => {
      const deletePointsSpy = mockSdkService("removePointFromNote", {
        details: "Updated content",
      })
      const wrapper = mount(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(false)
      await flushPromises()

      expect(deletePointsSpy).not.toHaveBeenCalled()
      expect(wrapper.emitted()).not.toHaveProperty("detailsUpdated")
    })
  })

  describe("LoadingModal for Promote Point", () => {
    it("shows LoadingModal while promoting point", async () => {
      let resolveApi: () => void
      const apiPromise = new Promise<void>((r) => {
        resolveApi = r
      })
      mockSdkServiceWithImplementation("promotePoint", async () => {
        await apiPromise
        return {
          createdNote: makeMe.aNoteRealm.please(),
          updatedParentNote: makeMe.aNoteRealm.please(),
        }
      })
      const wrapper = mount(["Test understanding point"])
      await flushPromises()

      await wrapper
        .find('button[title="Promote to child note"]')
        .trigger("click")
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      expect(document.body.textContent).toContain("AI is creating note...")
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })

    it("hides LoadingModal when API fails", async () => {
      mockSdkService("promotePoint", wrapSdkError({}))
      const wrapper = mount(["Test understanding point"])
      await flushPromises()

      await wrapper
        .find('button[title="Promote to child note"]')
        .trigger("click")
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      usePopups().popups.done(true)
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })
  })

  describe("LoadingModal for Delete Points", () => {
    it("shows LoadingModal while deleting points", async () => {
      let resolveApi: () => void
      mockSdkServiceWithImplementation("removePointFromNote", async () => {
        await new Promise<void>((r) => {
          resolveApi = r
        })
        return { details: "Updated content" }
      })
      const wrapper = mount(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      expect(document.body.textContent).toContain("AI is removing content...")
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })

    it("hides LoadingModal when delete API fails", async () => {
      let resolveApi: () => void
      mockSdkServiceWithImplementation("removePointFromNote", async () => {
        await new Promise<void>((r) => {
          resolveApi = r
        })
        return wrapSdkError("API Error")
      })
      const wrapper = mount(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })
  })

  describe("ignore questions", () => {
    it("disables ignore button when no points are selected", async () => {
      const wrapper = mount(["Point 1", "Point 2"])
      await flushPromises()
      expect(
        (
          wrapper.find('[data-test-id="ignore-understanding-points"]')
            .element as HTMLButtonElement
        ).disabled
      ).toBe(true)
    })

    it("enables ignore button when a point is selected", async () => {
      const wrapper = mount(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      expect(
        (
          wrapper.find('[data-test-id="ignore-understanding-points"]')
            .element as HTMLButtonElement
        ).disabled
      ).toBe(false)
    })

    it("shows confirmation when ignore button is clicked", async () => {
      const wrapper = mount(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="ignore-understanding-points"]')
        .trigger("click")
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups).toHaveLength(1)
      expect(popups[0]!.type).toBe("confirm")
      expect(popups[0]!.message).toContain("Ignore")
    })

    it("clears selection when confirm is clicked", async () => {
      const wrapper = mount(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="ignore-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()

      const checkboxes = wrapper.findAll('input[type="checkbox"]')
      expect(
        checkboxes.every((cb) => !(cb.element as HTMLInputElement).checked)
      )
    })

    it("keeps selection when cancel is clicked", async () => {
      const wrapper = mount(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="ignore-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(false)
      await flushPromises()

      const checkboxes = wrapper.findAll('input[type="checkbox"]')
      expect((checkboxes[0]?.element as HTMLInputElement).checked).toBe(true)
    })
  })
})
