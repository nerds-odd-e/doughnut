import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import NoteEditingHistory from "@/store/NoteEditingHistory"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, expect, vi } from "vitest"
import NoteUndoButton from "@/components/toolbars/NoteUndoButton.vue"

export const mockedPush = vi.fn()

export let noteEditingHistory: NoteEditingHistory

export function setupNoteUndoButtonTests() {
  beforeEach(() => {
    vi.clearAllMocks()
    noteEditingHistory = new NoteEditingHistory()
    const storageAccessor = useStorageAccessor()
    storageAccessor.value = createNoteStorage(noteEditingHistory)
  })
}

export function mountNoteUndoButton() {
  return helper.component(NoteUndoButton).mount()
}

export function renderNoteUndoButton() {
  return helper.component(NoteUndoButton).render()
}

export function refreshNoteRealms(...noteRealms: NoteRealm[]) {
  const storageAccessor = useStorageAccessor()
  for (const noteRealm of noteRealms) {
    storageAccessor.value.refreshNoteRealm(noteRealm)
  }
}

export function undoButtonEl(title: string) {
  return document.querySelector(`[title="${title}"]`) as HTMLElement | null
}

export function confirmUndoHeadingEl() {
  return document.querySelector(".undo-confirmation h2")
}

export function dialogOkButtonEl() {
  return document.querySelector(
    ".undo-confirmation .daisy-btn-success"
  ) as HTMLElement | null
}

export function dialogCancelButtonEl() {
  return document.querySelector(
    ".undo-confirmation .daisy-btn-secondary"
  ) as HTMLElement | null
}

export function dialogDiscardButtonEl() {
  return document.querySelector(
    ".undo-confirmation .daisy-btn-warning"
  ) as HTMLElement | null
}

export async function clickUndoButton(title: string) {
  const button = undoButtonEl(title)
  expect(button).toBeTruthy()
  button!.click()
  await flushPromises()
}

export async function clickDialogOk() {
  const button = dialogOkButtonEl()
  expect(button).toBeTruthy()
  button!.click()
  await flushPromises()
}

export async function clickDialogCancel() {
  const button = dialogCancelButtonEl()
  expect(button).toBeTruthy()
  button!.click()
  await flushPromises()
}

export async function clickDialogDiscard() {
  const button = dialogDiscardButtonEl()
  expect(button).toBeTruthy()
  button!.click()
  await flushPromises()
}

export function expectConfirmUndoVisible() {
  expect(confirmUndoHeadingEl()?.textContent).toBe("Confirm Undo")
}

export function expectConfirmUndoHidden() {
  expect(confirmUndoHeadingEl()).toBeNull()
}

export function expectNoteTitleLink(title: string) {
  const link = Array.from(document.querySelectorAll("a.router-link")).find(
    (anchor) => anchor.textContent?.includes(title)
  )
  expect(link).toBeTruthy()
}

export function expectNoteTitleVisible(title: string) {
  expect(document.body.textContent).toContain(title)
}

export function expectNoteTitleHidden(title: string) {
  const links = Array.from(document.querySelectorAll("a.router-link")).filter(
    (anchor) => anchor.textContent?.includes(title)
  )
  expect(links).toHaveLength(0)
}

export function setupTwoCachedNotes(
  firstTitle = "First Note",
  secondTitle = "Second Note"
) {
  const noteRealm1 = makeMe.aNoteRealm.title(firstTitle).please()
  const noteRealm2 = makeMe.aNoteRealm.title(secondTitle).please()
  refreshNoteRealms(noteRealm1, noteRealm2)
  return { noteRealm1, noteRealm2 }
}
