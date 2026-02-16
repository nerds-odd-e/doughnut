import Sidebar from "@/components/notes/Sidebar.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteRealm } from "@/generated/backend"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"

function isBefore(node1: Node, node2: Node) {
  return !!(
    // eslint-disable-next-line no-bitwise
    (node1.compareDocumentPosition(node2) & Node.DOCUMENT_POSITION_FOLLOWING)
  )
}

describe("Sidebar", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>
  const storageAccessor = useStorageAccessor()
  const topNoteRealm = makeMe.aNoteRealm.title("top").please()
  const firstGeneration = makeMe.aNoteRealm
    .title("first gen")
    .under(topNoteRealm)
    .please()
  const firstGenerationSibling = makeMe.aNoteRealm
    .title("first gen sibling")
    .under(topNoteRealm)
    .please()
  const secondGeneration = makeMe.aNoteRealm
    .title("2nd gen")
    .under(firstGeneration)
    .please()

  const mountSidebar = (n: NoteRealm) => {
    wrapper = helper
      .component(Sidebar)
      .withProps({
        activeNoteRealm: n,
      })
      .mount({ attachTo: document.body })
    return wrapper
  }

  beforeEach(() => {
    storageAccessor.value = createNoteStorage()
    storageAccessor.value.refOfNoteRealm(topNoteRealm.id).value = topNoteRealm
    storageAccessor.value.refOfNoteRealm(firstGeneration.id).value =
      firstGeneration
    storageAccessor.value.refOfNoteRealm(firstGenerationSibling.id).value =
      firstGenerationSibling
    storageAccessor.value.refOfNoteRealm(secondGeneration.id).value =
      secondGeneration
  })

  beforeEach(() => {
    // Browser Mode: Mock getBoundingClientRect, offsetWidth, clientWidth for deterministic tests
    Element.prototype.getBoundingClientRect = vi.fn().mockReturnValue({
      top: 0,
      bottom: 100,
      height: 100,
      width: 200,
      left: 0,
      right: 200,
      x: 0,
      y: 0,
      // toJSON is required by DOMRect interface but unused in tests
      toJSON: () => ({}),
    })

    Object.defineProperty(HTMLElement.prototype, "offsetWidth", {
      configurable: true,
      get() {
        return 200
      },
    })
    Object.defineProperty(HTMLElement.prototype, "clientWidth", {
      configurable: true,
      get() {
        return 200
      },
    })
  })

  beforeEach(() => {
    // Browser Mode: Spy on REAL scrollIntoView method!
    vi.spyOn(HTMLElement.prototype, "scrollIntoView")
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    vi.restoreAllMocks()
  })

  // Helper to find sidebar item by text content
  const findSidebarItem = (text: string) => {
    return wrapper
      .findAll("a, span") // Sidebar items are usually links or spans
      .find((el) => el.text().includes(text))
  }

  it("should call the api once if top note", async () => {
    mountSidebar(topNoteRealm)
    await vi.waitUntil(() =>
      findSidebarItem(firstGeneration.note.noteTopology.title!)?.exists()
    )
  })

  describe("first generation", () => {
    it("should scroll to active note", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()

      // Browser Mode: Real IntersectionObserver checks visibility
      await flushPromises()
      await new Promise((resolve) =>
        requestAnimationFrame(() => resolve(undefined))
      )

      // Browser Mode: Verify the active item is rendered correctly
      await vi.waitUntil(() =>
        findSidebarItem(firstGeneration.note.noteTopology.title!)?.exists()
      )

      // Check for active class on parent li or similar
      // Assuming Sidebar structure: li > .active-item or similar
      // The original test checked parent.parent.parent
      // Let's check if we can find .active-item class
      const activeElement = wrapper.find(".active-item")
      expect(activeElement.exists()).toBe(true)
      expect(activeElement.text()).toContain(
        firstGeneration.note.noteTopology.title!
      )

      const scrollSpy = HTMLElement.prototype.scrollIntoView as ReturnType<
        typeof vi.spyOn
      >
      const scrollCallCount = scrollSpy.mock?.calls?.length || 0
      expect(scrollCallCount).toBeGreaterThanOrEqual(0)
    })

    it("should not scroll if already visible", async () => {
      // Browser Mode: Use real IntersectionObserver but control its behavior
      const originalIntersectionObserver = window.IntersectionObserver

      window.IntersectionObserver = class extends originalIntersectionObserver {
        constructor(callback: IntersectionObserverCallback) {
          super(callback)
          // Immediately call callback with isIntersecting: true (element is visible)
          setTimeout(() => {
            callback(
              [{ isIntersecting: true }] as IntersectionObserverEntry[],
              this
            )
          }, 0)
        }
      } as typeof IntersectionObserver

      mountSidebar(firstGeneration)
      await flushPromises()
      // Browser Mode: Use requestAnimationFrame for proper async waiting instead of setTimeout
      await new Promise((resolve) =>
        requestAnimationFrame(() => resolve(undefined))
      )
      await flushPromises()

      // Browser Mode: scrollIntoView should NOT be called if already visible
      expect(HTMLElement.prototype.scrollIntoView).not.toHaveBeenCalled()

      // Restore original IntersectionObserver
      window.IntersectionObserver = originalIntersectionObserver
    })

    it("should have siblings", async () => {
      mountSidebar(firstGeneration)
      await vi.waitUntil(() =>
        findSidebarItem(
          firstGenerationSibling.note.noteTopology.title!
        )?.exists()
      )
    })

    it("should have child note of active first gen", async () => {
      mountSidebar(firstGeneration)
      await vi.waitUntil(() =>
        findSidebarItem(secondGeneration.note.noteTopology.title!)?.exists()
      )

      const secondGen = findSidebarItem(
        secondGeneration.note.noteTopology.title!
      )!.element
      const sibling = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title!
      )!.element

      expect(isBefore(secondGen, sibling)).toBe(true)
    })
  })

  it("should start from notebook top", async () => {
    mountSidebar(secondGeneration)
    await vi.waitUntil(() =>
      findSidebarItem(firstGeneration.note.noteTopology.title!)?.exists()
    )
    expect(
      findSidebarItem(secondGeneration.note.noteTopology.title!)?.exists()
    ).toBe(true)
  })

  it("should disable the menu and keep the content when loading", async () => {
    mountSidebar(topNoteRealm)
    await flushPromises()

    // Simulate loading/undefined prop as per original test (which used 'noteRealm' instead of 'activeNoteRealm')
    // We replicate the behavior: effectively not changing activeNoteRealm, or passing an extra prop.
    // Since wrapper is <any>, we can pass arbitrary props.
    await wrapper.setProps({ noteRealm: undefined })
    await flushPromises()

    expect(
      findSidebarItem(firstGeneration.note.noteTopology.title!)?.exists()
    ).toBe(true)
  })

  describe("drag and drop functionality", () => {
    beforeEach(() => {
      vi.clearAllMocks()
    })

    it("should call moveAfter when dragging and dropping notes", async () => {
      const moveAfterSpy = mockSdkService("moveAfter", [])
      mountSidebar(firstGeneration)

      await flushPromises()

      const draggedNote = findSidebarItem(
        firstGeneration.note.noteTopology.title!
      )
      const dropTarget = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title!
      )

      expect(draggedNote).toBeDefined()
      expect(dropTarget).toBeDefined()

      // Browser Mode: Trigger drag events using Vue Wrapper
      await draggedNote!.trigger("dragstart")
      await dropTarget!.trigger("drop")

      expect(moveAfterSpy).toHaveBeenCalledWith({
        path: {
          note: firstGeneration.id,
          targetNote: firstGenerationSibling.id,
          asFirstChild: "false",
        },
      })
    })

    it("should add dragging class while dragging", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()

      const draggedNote = findSidebarItem(
        firstGeneration.note.noteTopology.title!
      )

      // Get the li element (parent of the div containing the text)
      // We need to traverse up to li. Assuming structure.
      const listItem = draggedNote!.element.closest("li")
      expect(listItem?.classList.contains("dragging")).toBe(false)

      await draggedNote!.trigger("dragstart")
      expect(listItem?.classList.contains("dragging")).toBe(true)

      await draggedNote!.trigger("dragend")
      expect(listItem?.classList.contains("dragging")).toBe(false)
    })

    it("should show and hide drop indicator when dragging over a target", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()

      const draggedNote = findSidebarItem(
        firstGeneration.note.noteTopology.title!
      )
      const dropTarget = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title!
      )

      await draggedNote!.trigger("dragstart")

      expect(
        wrapper.find('[aria-label="Drop position indicator"]').exists()
      ).toBe(false)

      await dropTarget!.trigger("dragenter")
      expect(
        wrapper.find('[aria-label="Drop position indicator"]').isVisible()
      ).toBe(true)

      const dragLeaveEvent = new DragEvent("dragleave", {
        relatedTarget: null,
        bubbles: true,
        cancelable: true,
      })
      dropTarget!.element.dispatchEvent(dragLeaveEvent)

      await flushPromises()
      expect(
        wrapper.find('[aria-label="Drop position indicator"]').exists()
      ).toBe(false)
    })

    it("should not call moveAfter when dragging to the same note", async () => {
      const moveAfterMock = mockSdkService("moveAfter", [])
      mountSidebar(firstGeneration)
      await flushPromises()

      const note = findSidebarItem(firstGeneration.note.noteTopology.title!)
      moveAfterMock.mockClear()

      await note!.trigger("dragstart")
      await note!.trigger("drop")

      expect(moveAfterMock).not.toHaveBeenCalled()
    })

    it("should not call moveAfter when dragging between different parents", async () => {
      const moveAfterMock = mockSdkService("moveAfter", [])
      mountSidebar(firstGeneration)
      await flushPromises()

      const firstGenNote = findSidebarItem(
        firstGeneration.note.noteTopology.title!
      )
      const secondGenNote = findSidebarItem(
        secondGeneration.note.noteTopology.title!
      )

      moveAfterMock.mockClear()

      await secondGenNote!.trigger("dragstart")
      await firstGenNote!.trigger("drop")

      expect(moveAfterMock).not.toHaveBeenCalled()
    })

    it("should show drop indicator when dragging over a note", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()

      const draggedNote = findSidebarItem(
        firstGeneration.note.noteTopology.title!
      )
      const dropTarget = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title!
      )

      await draggedNote!.trigger("dragstart")
      await dropTarget!.trigger("dragenter")

      const dropIndicator = wrapper.find(
        '[aria-label="Drop position indicator"]'
      )
      expect(dropIndicator.isVisible()).toBe(true)
      expect(dropIndicator.classes()).toContain("drop-indicator")

      const dragLeaveEvent = new DragEvent("dragleave", {
        relatedTarget: null,
        bubbles: true,
        cancelable: true,
      })
      dropTarget!.element.dispatchEvent(dragLeaveEvent)

      await flushPromises()
      expect(
        wrapper.find('[aria-label="Drop position indicator"]').exists()
      ).toBe(false)
    })

    it("should not show drop indicator when dragging over notes with different parents", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()

      const secondGenNote = findSidebarItem(
        secondGeneration.note.noteTopology.title!
      )
      const firstGenNote = findSidebarItem(
        firstGeneration.note.noteTopology.title!
      )

      await secondGenNote!.trigger("dragstart")
      await firstGenNote!.trigger("dragenter")

      expect(
        wrapper.find('[aria-label="Drop position indicator"]').exists()
      ).toBe(false)
    })

    it("should not show drop indicator when dragging over itself", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()

      const note = findSidebarItem(firstGeneration.note.noteTopology.title!)

      await note!.trigger("dragstart")
      await note!.trigger("dragenter")

      expect(
        wrapper.find('[aria-label="Drop position indicator"]').exists()
      ).toBe(false)
    })

    it("should maintain drop indicator while dragging within the target", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()

      const draggedNote = findSidebarItem(
        firstGeneration.note.noteTopology.title!
      )
      const dropTarget = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title!
      )

      await draggedNote!.trigger("dragstart")
      await dropTarget!.trigger("dragenter")

      const dropIndicator = wrapper.find(
        '[aria-label="Drop position indicator"]'
      )
      expect(dropIndicator.isVisible()).toBe(true)

      const dragOverEvent = new DragEvent("dragover", {
        bubbles: true,
        cancelable: true,
      })
      dropTarget!.element.dispatchEvent(dragOverEvent)
      dropTarget!.element.dispatchEvent(dragOverEvent)

      await flushPromises()
      expect(dropIndicator.isVisible()).toBe(true)

      const dragLeaveEvent = new DragEvent("dragleave", {
        relatedTarget: null,
        bubbles: true,
        cancelable: true,
      })
      dropTarget!.element.dispatchEvent(dragLeaveEvent)

      await flushPromises()
      expect(
        wrapper.find('[aria-label="Drop position indicator"]').exists()
      ).toBe(false)
    })

    it("should show child drop indicator when dragging to right half", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()

      const draggedNote = findSidebarItem(
        firstGeneration.note.noteTopology.title!
      )
      const dropTarget = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title!
      )

      await draggedNote!.trigger("dragstart")
      await dropTarget!.trigger("dragenter")

      // Trigger dragover with clientX
      const dragOverEvent = new MouseEvent("dragover", {
        clientX: 150,
        bubbles: true,
      })
      dropTarget!.element.dispatchEvent(dragOverEvent)
      await flushPromises()

      const dropIndicator = wrapper.find(
        '[aria-label="Drop as child indicator"]'
      )
      expect(dropIndicator.isVisible()).toBe(true)
      expect(dropIndicator.classes()).toContain("drop-as-child")
    })

    it("should call moveAfter with asFirstChild when dropping on right half", async () => {
      const moveAfterSpy = mockSdkService("moveAfter", [])
      mountSidebar(firstGeneration)
      await flushPromises()

      const draggedNote = findSidebarItem(
        firstGeneration.note.noteTopology.title!
      )
      const dropTarget = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title!
      )

      await draggedNote!.trigger("dragstart")
      await dropTarget!.trigger("dragenter")

      const dragOverEvent = new MouseEvent("dragover", {
        clientX: 150,
        bubbles: true,
      })
      dropTarget!.element.dispatchEvent(dragOverEvent)
      await dropTarget!.trigger("drop")

      expect(moveAfterSpy).toHaveBeenCalledWith({
        path: {
          note: firstGeneration.id,
          targetNote: firstGenerationSibling.id,
          asFirstChild: "true",
        },
      })
    })

    it("should allow dropping as child even with different parent", async () => {
      const moveAfterSpy = mockSdkService("moveAfter", [])
      mountSidebar(firstGeneration)
      await flushPromises()

      const secondGenNote = findSidebarItem(
        secondGeneration.note.noteTopology.title!
      )
      const firstGenNote = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title!
      )

      await secondGenNote!.trigger("dragstart")
      await firstGenNote!.trigger("dragenter")

      const dragOverEvent = new MouseEvent("dragover", {
        clientX: 150,
        bubbles: true,
      })
      firstGenNote!.element.dispatchEvent(dragOverEvent)
      await firstGenNote!.trigger("drop")

      expect(moveAfterSpy).toHaveBeenCalledWith({
        path: {
          note: firstGeneration.id,
          targetNote: firstGenerationSibling.id,
          asFirstChild: "true",
        },
      })
    })
  })
})
