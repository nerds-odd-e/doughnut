import usePopups from "@/components/commons/Popups/usePopups"
import { flushPromises } from "@vue/test-utils"
import { expect } from "vitest"

/** Empties the real popup stack, dropping any popup a previous test left pending. */
export function emptyPopupStack() {
  usePopups().popups.register({ popupInfo: [] })
}

/** The popups currently waiting for an answer, oldest first. */
export function pendingPopups() {
  return usePopups().popups.peek()
}

/** The one popup waiting for an answer; fails when there is not exactly one. */
export function onlyPendingPopup() {
  const popups = pendingPopups()
  expect(popups).toHaveLength(1)
  return popups[0]
}

/** Answers the newest pending popup and lets the caller continue. */
export async function answerPopup(result: unknown) {
  usePopups().popups.done(result)
  await flushPromises()
}
