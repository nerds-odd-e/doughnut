import {
  NoteController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { describe, it, expect, vi } from "vitest"
import { screen } from "@testing-library/vue"

const mockedPush = vi.fn()
vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({ path: "/", fullPath: "/" }),
    useRouter: () => ({
      push: mockedPush,
    }),
  }
})
import {
  clickDialogCancel,
  clickDialogDiscard,
  clickDialogOk,
  clickUndoButton,
  expectConfirmUndoHidden,
  expectConfirmUndoVisible,
  expectNoteTitleHidden,
  expectNoteTitleLink,
  expectNoteTitleVisible,
  mountNoteUndoButton,
  noteEditingHistory,
  refreshNoteRealms,
  renderNoteUndoButton,
  setupNoteUndoButtonTests,
  setupTwoCachedNotes,
} from "./noteUndoButtonTestSupport"

setupNoteUndoButtonTests()

describe("NoteUndoButton", () => {
  it("does not show when there is nothing to undo", () => {
    const wrapper = mountNoteUndoButton()
    expect(wrapper.find("button").exists()).toBe(false)
  })

  it.each([
    {
      setup: (noteId: number) => noteEditingHistory.deleteNote(noteId),
      expectedTitle: "undo delete note",
    },
    {
      setup: (noteId: number) => noteEditingHistory.createNote(noteId),
      expectedTitle: "undo create note",
    },
    {
      setup: (noteId: number) =>
        noteEditingHistory.moveNote(noteId, { folderId: null, notebookId: 1 }),
      expectedTitle: "undo move note",
    },
  ])(
    "shows with title $expectedTitle when undo is available",
    ({ setup, expectedTitle }) => {
      const note = makeMe.aNote.please()
      setup(note.id)
      const wrapper = mountNoteUndoButton()
      expect(wrapper.find("button").exists()).toBe(true)
      expect(wrapper.find("button").attributes("title")).toBe(expectedTitle)
    }
  )

  describe("confirmation dialog", () => {
    describe("when note is in cache", () => {
      it.each([
        {
          action: "delete note",
          setup: (noteRealm: ReturnType<typeof makeMe.aNoteRealm.please>) =>
            noteEditingHistory.deleteNote(noteRealm.id),
          undoTitle: "undo delete note",
          message: /Are you sure you want to undo deleting /,
        },
        {
          action: "create note",
          setup: (noteRealm: ReturnType<typeof makeMe.aNoteRealm.please>) =>
            noteEditingHistory.createNote(noteRealm.id),
          undoTitle: "undo create note",
          message: /Are you sure you want to undo creating /,
        },
        {
          action: "move note",
          setup: (noteRealm: ReturnType<typeof makeMe.aNoteRealm.please>) => {
            noteEditingHistory.moveNote(noteRealm.id, {
              folderId: null,
              notebookId: noteRealm.notebookRealm.notebook.id,
            })
          },
          undoTitle: "undo move note",
          message: /Are you sure you want to undo moving /,
        },
      ])(
        "shows confirmation dialog with note title for $action",
        async ({ setup, undoTitle, message }) => {
          const noteRealm = makeMe.aNoteRealm.title("My Note").please()
          refreshNoteRealms(noteRealm)
          setup(noteRealm)
          renderNoteUndoButton()

          await clickUndoButton(undoTitle)

          expectConfirmUndoVisible()
          expect(screen.getByText(message)).toBeInTheDocument()
          expectNoteTitleVisible("My Note")
          expectNoteTitleLink("My Note")
        }
      )

      it("shows confirmation dialog with note title and diff for edit title", async () => {
        const noteRealm = makeMe.aNoteRealm.title("Test Note").please()
        refreshNoteRealms(noteRealm)
        noteEditingHistory.addEditingToUndoHistory(
          noteRealm.id,
          "edit title",
          "Old Title"
        )
        renderNoteUndoButton()

        await clickUndoButton("undo edit title")

        expectConfirmUndoVisible()
        expect(
          screen.getByText(
            /Are you sure you want to undo editing the title of /
          )
        ).toBeInTheDocument()
        expectNoteTitleLink("Test Note")
        expect(screen.getByText("Current")).toBeInTheDocument()
        expect(screen.getByText("Will restore to")).toBeInTheDocument()
      })

      it("shows confirmation dialog with note title and diff for edit content", async () => {
        const noteRealm = makeMe.aNoteRealm.title("Content Note").please()
        refreshNoteRealms(noteRealm)
        noteEditingHistory.addEditingToUndoHistory(
          noteRealm.id,
          "edit content",
          "Old Content"
        )
        renderNoteUndoButton()

        await clickUndoButton("undo edit content")

        expectConfirmUndoVisible()
        expect(
          screen.getByText(
            /Are you sure you want to undo editing the content of /
          )
        ).toBeInTheDocument()
        expectNoteTitleVisible("Content Note")
        expectNoteTitleLink("Content Note")
        expect(screen.getByText("Current")).toBeInTheDocument()
        expect(screen.getByText("Will restore to")).toBeInTheDocument()
      })

      it("shows diff view for edit content with long content", async () => {
        const noteRealm = makeMe.aNoteRealm.title("Content Note").please()
        refreshNoteRealms(noteRealm)
        noteEditingHistory.addEditingToUndoHistory(
          noteRealm.id,
          "edit content",
          "A".repeat(150)
        )
        renderNoteUndoButton()

        await clickUndoButton("undo edit content")

        expect(screen.getByText("Current")).toBeInTheDocument()
        expect(screen.getByText("Will restore to")).toBeInTheDocument()
      })

      it("shows HTML tags as part of markdown content in diff view", async () => {
        const noteRealm = makeMe.aNoteRealm.title("Content Note").please()
        refreshNoteRealms(noteRealm)
        noteEditingHistory.addEditingToUndoHistory(
          noteRealm.id,
          "edit content",
          "<p>Old <strong>Content</strong> with <em>HTML</em></p>"
        )
        renderNoteUndoButton()

        await clickUndoButton("undo edit content")

        expect(screen.getByText("Will restore to")).toBeInTheDocument()
        const diffContent = screen.getByText("Will restore to").parentElement
        expect(diffContent?.textContent).toContain(
          "<p>Old <strong>Content</strong> with <em>HTML</em></p>"
        )
      })
    })

    describe("when note is not in cache", () => {
      it.each([
        {
          action: "delete note",
          setup: (noteId: number) => noteEditingHistory.deleteNote(noteId),
          undoTitle: "undo delete note",
          assertMessage: (noteId: number) =>
            expect(
              screen.getByText(
                new RegExp(
                  `Are you sure you want to undo deleting note id: ${noteId}\\?`
                )
              )
            ).toBeInTheDocument(),
        },
        {
          action: "edit title",
          setup: (noteId: number) =>
            noteEditingHistory.addEditingToUndoHistory(
              noteId,
              "edit title",
              "Old Title"
            ),
          undoTitle: "undo edit title",
          assertMessage: () => undefined,
        },
        {
          action: "edit content",
          setup: (noteId: number) =>
            noteEditingHistory.addEditingToUndoHistory(
              noteId,
              "edit content",
              "Old Content"
            ),
          undoTitle: "undo edit content",
          assertMessage: () => undefined,
        },
      ])(
        "shows confirmation dialog for $action when note is not cached",
        async ({ setup, undoTitle, assertMessage }) => {
          const note = makeMe.aNote.please()
          setup(note.id)
          renderNoteUndoButton()

          await clickUndoButton(undoTitle)

          expectConfirmUndoVisible()
          assertMessage(note.id)
          if (undoTitle !== "undo delete note") {
            expect(screen.getByText("Current")).toBeInTheDocument()
            expect(screen.getByText("Will restore to")).toBeInTheDocument()
          }
        }
      )
    })

    it.each([
      {
        label: "delete note",
        setup: () => {
          const note = makeMe.aNote.please()
          const noteRealm = makeMe.aNoteRealm.please()
          noteEditingHistory.deleteNote(note.id)
          mockSdkService(NoteController, "undoDeleteNote", noteRealm)
          return { noteRealm, undoTitle: "undo delete note" }
        },
        expectedNoteId: (result: {
          noteRealm: ReturnType<typeof makeMe.aNoteRealm.please>
        }) => result.noteRealm.id,
      },
      {
        label: "edit title",
        setup: () => {
          const note = makeMe.aNote.please()
          const noteRealm = makeMe.aNoteRealm.please()
          noteEditingHistory.addEditingToUndoHistory(
            note.id,
            "edit title",
            "Old Title"
          )
          mockSdkService(TextContentController, "updateNoteTitle", noteRealm)
          return { noteRealm, undoTitle: "undo edit title" }
        },
        expectedNoteId: (result: {
          noteRealm: ReturnType<typeof makeMe.aNoteRealm.please>
        }) => result.noteRealm.id,
      },
      {
        label: "edit content without prior content",
        setup: () => {
          const note = makeMe.aNote.please()
          const noteRealm = makeMe.aNoteRealm.please()
          noteEditingHistory.addEditingToUndoHistory(
            note.id,
            "edit content",
            undefined
          )
          mockSdkService(TextContentController, "updateNoteContent", noteRealm)
          return { noteRealm, undoTitle: "undo edit content" }
        },
        expectedNoteId: (result: {
          noteRealm: ReturnType<typeof makeMe.aNoteRealm.please>
        }) => result.noteRealm.id,
      },
      {
        label: "create note",
        setup: () => {
          const noteRealm = makeMe.aNoteRealm.please()
          const parentNoteRealm = makeMe.aNoteRealm.please()
          noteEditingHistory.createNote(noteRealm.id)
          mockSdkService(NoteController, "deleteNote", [parentNoteRealm])
          return {
            noteRealm: parentNoteRealm,
            undoTitle: "undo create note",
          }
        },
        expectedNoteId: (result: {
          noteRealm: ReturnType<typeof makeMe.aNoteRealm.please>
        }) => result.noteRealm.id,
      },
    ])(
      "calls undo when confirmation is accepted for $label",
      async ({ setup, expectedNoteId }) => {
        const { noteRealm, undoTitle } = setup()
        renderNoteUndoButton()

        await clickUndoButton(undoTitle)
        await clickDialogOk()

        expect(mockedPush).toHaveBeenCalledWith({
          name: "noteShow",
          params: {
            noteId: String(expectedNoteId({ noteRealm })),
          },
        })
      }
    )

    it("does not call undo when confirmation is cancelled", async () => {
      const note = makeMe.aNote.please()
      noteEditingHistory.deleteNote(note.id)
      renderNoteUndoButton()

      await clickUndoButton("undo delete note")
      await clickDialogCancel()

      expect(mockedPush).not.toHaveBeenCalled()
    })

    describe("discard functionality", () => {
      it.each([
        {
          action: "delete note",
          setup: (
            noteRealm1: ReturnType<typeof makeMe.aNoteRealm.please>,
            noteRealm2: ReturnType<typeof makeMe.aNoteRealm.please>
          ) => {
            noteEditingHistory.deleteNote(noteRealm2.id)
            noteEditingHistory.deleteNote(noteRealm1.id)
          },
          undoTitle: "undo delete note",
        },
        {
          action: "edit title",
          setup: (
            noteRealm1: ReturnType<typeof makeMe.aNoteRealm.please>,
            noteRealm2: ReturnType<typeof makeMe.aNoteRealm.please>
          ) => {
            noteEditingHistory.addEditingToUndoHistory(
              noteRealm2.id,
              "edit title",
              "Old Title 2"
            )
            noteEditingHistory.addEditingToUndoHistory(
              noteRealm1.id,
              "edit title",
              "Old Title 1"
            )
          },
          undoTitle: "undo edit title",
        },
        {
          action: "edit content",
          setup: (
            noteRealm1: ReturnType<typeof makeMe.aNoteRealm.please>,
            noteRealm2: ReturnType<typeof makeMe.aNoteRealm.please>
          ) => {
            noteEditingHistory.addEditingToUndoHistory(
              noteRealm2.id,
              "edit content",
              "Old Content 2"
            )
            noteEditingHistory.addEditingToUndoHistory(
              noteRealm1.id,
              "edit content",
              "Old Content 1"
            )
          },
          undoTitle: "undo edit content",
        },
      ])(
        "discards $action item and shows next item",
        async ({ setup, undoTitle }) => {
          const { noteRealm1, noteRealm2 } = setupTwoCachedNotes()
          setup(noteRealm1, noteRealm2)
          renderNoteUndoButton()

          await clickUndoButton(undoTitle)

          expectConfirmUndoVisible()
          expectNoteTitleVisible("First Note")

          await clickDialogDiscard()

          expectConfirmUndoVisible()
          expectNoteTitleVisible("Second Note")
          expectNoteTitleHidden("First Note")
        }
      )

      it("closes dialog when discarding the last undo item", async () => {
        const note = makeMe.aNote.please()
        noteEditingHistory.deleteNote(note.id)
        renderNoteUndoButton()

        await clickUndoButton("undo delete note")
        expectConfirmUndoVisible()

        await clickDialogDiscard()

        expectConfirmUndoHidden()
      })
    })
  })
})
