import { screen } from "@testing-library/vue"
import "intersection-observer"
import Sidebar from "@/components/notes/Sidebar.vue"
import type { NoteRealm } from "@/generated/backend"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { fireEvent } from "@testing-library/vue"

function isBefore(node1: Node, node2: Node) {
  return !!(
    // eslint-disable-next-line no-bitwise
    (node1.compareDocumentPosition(node2) & Node.DOCUMENT_POSITION_FOLLOWING)
  )
}

describe("Sidebar", () => {
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

  const render = (n: NoteRealm) => {
    return helper
      .component(Sidebar)
      .withStorageProps({
        noteRealm: n,
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

    // biome-ignore lint/suspicious/noExplicitAny: <explanation>
    global.IntersectionObserver = MockIntersectionObserver as any
    /* eslint-enable */
  })

  beforeEach(() => {
    window.HTMLElement.prototype.scrollIntoView = vitest.fn()
  })

  it("should call the api once if top note", async () => {
    helper.managedApi.restNoteController.show1 = vitest
      .fn()
      .mockResolvedValueOnce(topNoteRealm)
    render(topNoteRealm)
    expect(helper.managedApi.restNoteController.show1).toBeCalled()
    await screen.findByText(firstGeneration.note.noteTopic.topicConstructor)
  })

  describe("first generation", () => {
    beforeEach(() => {
      helper.managedApi.restNoteController.show1 = vitest
        .fn()
        .mockResolvedValueOnce(topNoteRealm)
        .mockResolvedValueOnce(firstGeneration)
    })

    it("should call the api if not top note", async () => {
      render(firstGeneration)
      await flushPromises()
      expect(helper.managedApi.restNoteController.show1).toBeCalledWith(
        topNoteRealm.id
      )
      expect(helper.managedApi.restNoteController.show1).toBeCalledWith(
        firstGeneration.id
      )
    })

    it("should scroll to active note", async () => {
      render(firstGeneration)
      await flushPromises()
      expect(window.HTMLElement.prototype.scrollIntoView).toBeCalled()
      expect(observerDisconnected).toBe(true)
      expect(
        /* eslint-disable */
        (
          await screen.findByText(
            firstGeneration.note.noteTopic.topicConstructor
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
        firstGenerationSibling.note.noteTopic.topicConstructor
      )
    })

    it("should have child note of active first gen", async () => {
      render(firstGeneration)
      const secondGen = await screen.findByText(
        secondGeneration.note.noteTopic.topicConstructor
      )
      const sibling = await screen.findByText(
        firstGenerationSibling.note.noteTopic.topicConstructor
      )
      expect(isBefore(secondGen, sibling)).toBe(true)
    })
  })

  it("should start from notebook top", async () => {
    helper.managedApi.restNoteController.show1 = vitest
      .fn()
      .mockResolvedValueOnce(topNoteRealm)
      .mockResolvedValueOnce(firstGeneration)
      .mockResolvedValueOnce(secondGeneration)
    render(secondGeneration)
    await screen.findByText(firstGeneration.note.noteTopic.topicConstructor)
    await screen.findByText(secondGeneration.note.noteTopic.topicConstructor)
  })

  it("should disable the menu and keep the content when loading", async () => {
    helper.managedApi.restNoteController.show1 = vitest
      .fn()
      .mockResolvedValueOnce(topNoteRealm)
    const { rerender } = render(topNoteRealm)
    await flushPromises()
    await rerender({ noteRealm: undefined })
    await flushPromises()
    await screen.findByText(firstGeneration.note.noteTopic.topicConstructor)
  })

  describe("drag and drop functionality", () => {
    beforeEach(() => {
      helper.managedApi.restNoteController.show1 = vitest
        .fn()
        .mockResolvedValueOnce(topNoteRealm)
        .mockResolvedValueOnce(firstGeneration)
    })

    it("should call moveAfter when dragging and dropping notes", async () => {
      helper.managedApi.restNoteController.moveAfter = vitest
        .fn()
        .mockResolvedValue([])
      render(firstGeneration)

      await flushPromises()

      const draggedNote = await screen.findByText(
        firstGeneration.note.noteTopic.topicConstructor
      )
      const dropTarget = await screen.findByText(
        firstGenerationSibling.note.noteTopic.topicConstructor
      )

      // Start drag
      await fireEvent.dragStart(draggedNote)

      // Perform drop
      await fireEvent.drop(dropTarget)

      expect(
        helper.managedApi.restNoteController.moveAfter
      ).toHaveBeenCalledWith(
        firstGeneration.id,
        firstGenerationSibling.id,
        "after"
      )
    })

    it("should add dragging class while dragging", async () => {
      render(firstGeneration)
      await flushPromises()

      const draggedNote = await screen.findByText(
        firstGeneration.note.noteTopic.topicConstructor
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

    it("should add drag-over class when dragging over a target", async () => {
      render(firstGeneration)
      await flushPromises()

      const dropTarget = await screen.findByText(
        firstGenerationSibling.note.noteTopic.topicConstructor
      )
      const listItem = dropTarget.closest("li")
      expect(listItem).not.toHaveClass("drag-over")

      // Drag enter
      await fireEvent.dragEnter(dropTarget)
      expect(listItem).toHaveClass("drag-over")

      // Drag leave
      await fireEvent.dragLeave(dropTarget)
      expect(listItem).not.toHaveClass("drag-over")
    })

    it("should not call moveAfter when dragging to the same note", async () => {
      helper.managedApi.restNoteController.moveAfter = vitest.fn()
      render(firstGeneration)
      await flushPromises()

      const note = await screen.findByText(
        firstGeneration.note.noteTopic.topicConstructor
      )

      // Drag and drop on itself
      await fireEvent.dragStart(note)
      await fireEvent.drop(note)

      expect(
        helper.managedApi.restNoteController.moveAfter
      ).not.toHaveBeenCalled()
    })

    it("should not call moveAfter when dragging between different parents", async () => {
      helper.managedApi.restNoteController.moveAfter = vitest.fn()
      render(firstGeneration)
      await flushPromises()

      const firstGenNote = await screen.findByText(
        firstGeneration.note.noteTopic.topicConstructor
      )
      const secondGenNote = await screen.findByText(
        secondGeneration.note.noteTopic.topicConstructor
      )

      // Try to drag between different parent levels
      await fireEvent.dragStart(secondGenNote)
      await fireEvent.drop(firstGenNote)

      expect(
        helper.managedApi.restNoteController.moveAfter
      ).not.toHaveBeenCalled()
    })

    it("should handle errors during moveAfter", async () => {
      const consoleError = vitest.spyOn(console, "error")
      helper.managedApi.restNoteController.moveAfter = vitest
        .fn()
        .mockRejectedValue(new Error("API Error"))

      render(firstGeneration)
      await flushPromises()

      const draggedNote = await screen.findByText(
        firstGeneration.note.noteTopic.topicConstructor
      )
      const dropTarget = await screen.findByText(
        firstGenerationSibling.note.noteTopic.topicConstructor
      )

      await fireEvent.dragStart(draggedNote)
      await fireEvent.drop(dropTarget)

      expect(consoleError).toHaveBeenCalledWith(
        "Failed to move note:",
        expect.any(Error)
      )
      consoleError.mockRestore()
    })
  })
})
