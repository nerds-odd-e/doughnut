import { screen } from "@testing-library/vue"
import Sidebar from "@/components/notes/Sidebar.vue"
import type { NoteRealm } from "@/generated/backend"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { fireEvent } from "@testing-library/vue"
import createNoteStorage from "@/store/createNoteStorage"

function isBefore(node1: Node, node2: Node) {
  return !!(
    // eslint-disable-next-line no-bitwise
    (node1.compareDocumentPosition(node2) & Node.DOCUMENT_POSITION_FOLLOWING)
  )
}

describe("Sidebar", () => {
  const storageAccessor = createNoteStorage(helper.managedApi)
  const topNoteRealm = makeMe.aNoteRealm.topicConstructor("top").please()
  const firstGeneration = makeMe.aNoteRealm
    .topicConstructor("first gen")
    .under(topNoteRealm)
    .please()
  const firstGenerationSibling = makeMe.aNoteRealm
    .topicConstructor("first gen sibling")
    .under(topNoteRealm)
    .please()
  const secondGeneration = makeMe.aNoteRealm
    .topicConstructor("2nd gen")
    .under(firstGeneration)
    .please()

  storageAccessor.refOfNoteRealm(topNoteRealm.id).value = topNoteRealm
  storageAccessor.refOfNoteRealm(firstGeneration.id).value = firstGeneration
  storageAccessor.refOfNoteRealm(firstGenerationSibling.id).value =
    firstGenerationSibling
  storageAccessor.refOfNoteRealm(secondGeneration.id).value = secondGeneration

  const render = (n: NoteRealm) => {
    return helper
      .component(Sidebar)
      .withProps({
        storageAccessor,
        activeNoteRealm: n,
      })
      .render()
  }

  let isIntersecting = false
  let observerDisconnected = false

  beforeEach(() => {
    isIntersecting = false
    observerDisconnected = false
    // Mock the IntersectionObserver
    /* eslint-disable */
    let observeCallback: IntersectionObserverCallback
    const MockIntersectionObserver = class {
      constructor(callback: IntersectionObserverCallback) {
        observeCallback = callback
      }

      observe() {
        // Call the callback with a mock entry
        observeCallback(
          [{ isIntersecting }] as IntersectionObserverEntry[],
          {} as IntersectionObserver
        )
      }

      disconnect() {
        observerDisconnected = true
      }

      unobserve() {
        // noop
      }
    }

    // biome-ignore lint/suspicious/noExplicitAny: Mock class doesn't fully implement IntersectionObserver interface
    global.IntersectionObserver = MockIntersectionObserver as any
    /* eslint-enable */

    // Mock getBoundingClientRect with width
    Element.prototype.getBoundingClientRect = vitest.fn().mockReturnValue({
      top: 0,
      bottom: 100,
      height: 100,
      width: 200, // Add width
      left: 0, // Add left position
    })

    // Mock offsetWidth and clientWidth
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
    window.HTMLElement.prototype.scrollIntoView = vitest.fn()
  })

  it("should call the api once if top note", async () => {
    render(topNoteRealm)
    await screen.findByText(firstGeneration.note.noteTopology.titleOrPredicate)
  })

  describe("first generation", () => {
    it("should scroll to active note", async () => {
      render(firstGeneration)
      await flushPromises()
      expect(window.HTMLElement.prototype.scrollIntoView).toBeCalled()
      expect(observerDisconnected).toBe(true)
      expect(
        /* eslint-disable */
        (
          await screen.findByText(
            firstGeneration.note.noteTopology.titleOrPredicate
          )
        ).parentNode?.parentNode?.parentNode
        /* eslint-enable */
      ).toHaveClass("active-item")
    })

    it("should not scroll if already visible", async () => {
      isIntersecting = true
      render(firstGeneration)
      await flushPromises()
      expect(window.HTMLElement.prototype.scrollIntoView).not.toBeCalled()
      expect(observerDisconnected).toBe(true)
    })

    it("should have siblings", async () => {
      render(firstGeneration)
      await screen.findByText(
        firstGenerationSibling.note.noteTopology.titleOrPredicate
      )
    })

    it("should have child note of active first gen", async () => {
      render(firstGeneration)
      const secondGen = await screen.findByText(
        secondGeneration.note.noteTopology.titleOrPredicate
      )
      const sibling = await screen.findByText(
        firstGenerationSibling.note.noteTopology.titleOrPredicate
      )
      expect(isBefore(secondGen, sibling)).toBe(true)
    })
  })

  it("should start from notebook top", async () => {
    render(secondGeneration)
    await screen.findByText(firstGeneration.note.noteTopology.titleOrPredicate)
    await screen.findByText(secondGeneration.note.noteTopology.titleOrPredicate)
  })

  it("should disable the menu and keep the content when loading", async () => {
    const { rerender } = render(topNoteRealm)
    await flushPromises()
    await rerender({ noteRealm: undefined })
    await flushPromises()
    await screen.findByText(firstGeneration.note.noteTopology.titleOrPredicate)
  })

  describe("drag and drop functionality", () => {
    beforeEach(() => {
      vi.clearAllMocks()
    })

    it("should call moveAfter when dragging and dropping notes", async () => {
      vi.spyOn(helper.managedApi.services, "moveAfter").mockResolvedValue([])
      render(firstGeneration)

      await flushPromises()

      const draggedNote = await screen.findByText(
        firstGeneration.note.noteTopology.titleOrPredicate
      )
      const dropTarget = await screen.findByText(
        firstGenerationSibling.note.noteTopology.titleOrPredicate
      )

      // Start drag
      await fireEvent.dragStart(draggedNote)

      // Perform drop
      await fireEvent.drop(dropTarget)

      expect(helper.managedApi.services.moveAfter).toHaveBeenCalledWith({
        note: firstGeneration.id,
        targetNote: firstGenerationSibling.id,
        asFirstChild: "false",
      })
    })

    it("should add dragging class while dragging", async () => {
      render(firstGeneration)
      await flushPromises()

      const draggedNote = await screen.findByText(
        firstGeneration.note.noteTopology.titleOrPredicate
      )

      // Get the li element (parent of the div containing the text)
      const listItem = draggedNote.closest("li")
      expect(listItem).not.toHaveClass("dragging")

      // Start drag
      await fireEvent.dragStart(draggedNote)
      expect(listItem).toHaveClass("dragging")

      // End drag
      await fireEvent.dragEnd(draggedNote)
      expect(listItem).not.toHaveClass("dragging")
    })

    it("should show and hide drop indicator when dragging over a target", async () => {
      render(firstGeneration)
      await flushPromises()

      const draggedNote = await screen.findByText(
        firstGeneration.note.noteTopology.titleOrPredicate
      )
      const dropTarget = await screen.findByText(
        firstGenerationSibling.note.noteTopology.titleOrPredicate
      )

      // Start drag
      await fireEvent.dragStart(draggedNote)

      // Before drag enter
      expect(
        screen.queryByRole("presentation", {
          name: "Drop position indicator",
        })
      ).not.toBeInTheDocument()

      // After drag enter
      await fireEvent.dragEnter(dropTarget)
      expect(
        screen.getByRole("presentation", {
          name: "Drop position indicator",
        })
      ).toBeVisible()

      // After drag leave
      await fireEvent.dragLeave(dropTarget)
      expect(
        screen.queryByRole("presentation", {
          name: "Drop position indicator",
        })
      ).not.toBeInTheDocument()
    })

    it("should not call moveAfter when dragging to the same note", async () => {
      const moveAfterMock = vi
        .spyOn(helper.managedApi.services, "moveAfter")
        .mockResolvedValue({} as never)
      render(firstGeneration)
      await flushPromises()

      const note = await screen.findByText(
        firstGeneration.note.noteTopology.titleOrPredicate
      )

      // Clear any calls from setup
      moveAfterMock.mockClear()

      // Drag and drop on itself
      await fireEvent.dragStart(note)
      await fireEvent.drop(note)

      expect(moveAfterMock).not.toHaveBeenCalled()
    })

    it("should not call moveAfter when dragging between different parents", async () => {
      const moveAfterMock = vi
        .spyOn(helper.managedApi.services, "moveAfter")
        .mockResolvedValue({} as never)
      render(firstGeneration)
      await flushPromises()

      const firstGenNote = await screen.findByText(
        firstGeneration.note.noteTopology.titleOrPredicate
      )
      const secondGenNote = await screen.findByText(
        secondGeneration.note.noteTopology.titleOrPredicate
      )

      // Clear any calls from setup
      moveAfterMock.mockClear()

      // Try to drag between different parent levels
      await fireEvent.dragStart(secondGenNote)
      await fireEvent.drop(firstGenNote)

      expect(moveAfterMock).not.toHaveBeenCalled()
    })

    it("should show drop indicator when dragging over a note", async () => {
      render(firstGeneration)
      await flushPromises()

      const draggedNote = await screen.findByText(
        firstGeneration.note.noteTopology.titleOrPredicate
      )
      const dropTarget = await screen.findByText(
        firstGenerationSibling.note.noteTopology.titleOrPredicate
      )

      // Start drag
      await fireEvent.dragStart(draggedNote)

      // Drag over target
      await fireEvent.dragEnter(dropTarget)

      // Check for drop indicator
      const dropIndicator = screen.getByRole("presentation", {
        name: "Drop position indicator",
      })
      expect(dropIndicator).toBeVisible()
      expect(dropIndicator).toHaveClass("drop-indicator")

      // Indicator should disappear after drag leave
      await fireEvent.dragLeave(dropTarget)
      expect(dropIndicator).not.toBeVisible()
    })

    it("should not show drop indicator when dragging over notes with different parents", async () => {
      render(firstGeneration)
      await flushPromises()

      const secondGenNote = await screen.findByText(
        secondGeneration.note.noteTopology.titleOrPredicate
      )
      const firstGenNote = await screen.findByText(
        firstGeneration.note.noteTopology.titleOrPredicate
      )

      // Start drag from second generation
      await fireEvent.dragStart(secondGenNote)

      // Try to drag over first generation
      await fireEvent.dragEnter(firstGenNote)

      // Check that drop indicator is not shown
      const dropIndicator = screen.queryByRole("presentation", {
        name: "Drop position indicator",
      })
      expect(dropIndicator).not.toBeInTheDocument()
    })

    it("should not show drop indicator when dragging over itself", async () => {
      render(firstGeneration)
      await flushPromises()

      const note = await screen.findByText(
        firstGeneration.note.noteTopology.titleOrPredicate
      )

      // Start drag
      await fireEvent.dragStart(note)

      // Drag over itself
      await fireEvent.dragEnter(note)

      // Check that drop indicator is not shown
      const dropIndicator = screen.queryByRole("presentation", {
        name: "Drop position indicator",
      })
      expect(dropIndicator).not.toBeInTheDocument()
    })

    it("should maintain drop indicator while dragging within the target", async () => {
      render(firstGeneration)
      await flushPromises()

      const draggedNote = await screen.findByText(
        firstGeneration.note.noteTopology.titleOrPredicate
      )
      const dropTarget = await screen.findByText(
        firstGenerationSibling.note.noteTopology.titleOrPredicate
      )

      // Start drag
      await fireEvent.dragStart(draggedNote)
      await fireEvent.dragEnter(dropTarget)

      const dropIndicator = screen.getByRole("presentation", {
        name: "Drop position indicator",
      })
      expect(dropIndicator).toBeVisible()

      // Multiple dragOver events shouldn't remove the indicator
      await fireEvent.dragOver(dropTarget)
      await fireEvent.dragOver(dropTarget)
      expect(dropIndicator).toBeVisible()

      // Only dragleave should remove it
      await fireEvent.dragLeave(dropTarget)
      expect(dropIndicator).not.toBeVisible()
    })

    it("should show child drop indicator when dragging to right half", async () => {
      render(firstGeneration)
      await flushPromises()

      const draggedNote = await screen.findByText(
        firstGeneration.note.noteTopology.titleOrPredicate
      )
      const dropTarget = await screen.findByText(
        firstGenerationSibling.note.noteTopology.titleOrPredicate
      )

      // Start drag
      await fireEvent.dragStart(draggedNote)

      // Drag over right half
      await fireEvent.dragEnter(dropTarget)
      const dragOverEvent = new MouseEvent("dragover", {
        clientX: 150,
        bubbles: true,
      })
      await fireEvent(dropTarget, dragOverEvent)

      const dropIndicator = screen.getByRole("presentation", {
        name: "Drop as child indicator",
      })
      expect(dropIndicator).toBeVisible()
      expect(dropIndicator).toHaveClass("drop-as-child")
    })

    it("should call moveAfter with asFirstChild when dropping on right half", async () => {
      vi.spyOn(helper.managedApi.services, "moveAfter").mockResolvedValue([])
      render(firstGeneration)
      await flushPromises()

      const draggedNote = await screen.findByText(
        firstGeneration.note.noteTopology.titleOrPredicate
      )
      const dropTarget = await screen.findByText(
        firstGenerationSibling.note.noteTopology.titleOrPredicate
      )

      // Start drag
      await fireEvent.dragStart(draggedNote)

      // Drag over right half and drop
      await fireEvent.dragEnter(dropTarget)
      const dragOverEvent = new MouseEvent("dragover", {
        clientX: 150,
        bubbles: true,
      })
      await fireEvent(dropTarget, dragOverEvent)
      await fireEvent.drop(dropTarget)

      expect(helper.managedApi.services.moveAfter).toHaveBeenCalledWith({
        note: firstGeneration.id,
        targetNote: firstGenerationSibling.id,
        asFirstChild: "true",
      })
    })

    it("should allow dropping as child even with different parent", async () => {
      vi.spyOn(helper.managedApi.services, "moveAfter").mockResolvedValue([])
      render(firstGeneration)
      await flushPromises()

      const secondGenNote = await screen.findByText(
        secondGeneration.note.noteTopology.titleOrPredicate
      )
      const firstGenNote = await screen.findByText(
        firstGenerationSibling.note.noteTopology.titleOrPredicate
      )

      // Start drag from second generation
      await fireEvent.dragStart(secondGenNote)

      // Drag over right half and drop
      await fireEvent.dragEnter(firstGenNote)
      const dragOverEvent = new MouseEvent("dragover", {
        clientX: 150,
        bubbles: true,
      })
      await fireEvent(firstGenNote, dragOverEvent)
      await fireEvent.drop(firstGenNote)

      expect(helper.managedApi.services.moveAfter).toHaveBeenCalledWith({
        note: firstGeneration.id,
        targetNote: firstGenerationSibling.id,
        asFirstChild: "true",
      })
    })
  })
})
