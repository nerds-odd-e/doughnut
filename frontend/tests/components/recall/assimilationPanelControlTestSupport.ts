import { flushPromises, type VueWrapper } from "@vue/test-utils"
import usePopups from "@/components/commons/Popups/usePopups"

export const assimilateButtonSelector = '[data-test="assimilate"]' as const
export const skipButtonSelector = '[data-test="skip"]' as const
export const returnToSequenceButtonSelector =
  '[data-test="return-to-sequence"]' as const
export const reviveButtonSelector = '[data-test="revive"]' as const
export const removeFromRecallButtonSelector =
  '[data-test="remove-from-recall"]' as const
export const assimilateOptionsCaretSelector =
  '[data-test="assimilate-options-caret"]' as const

export function assimilateButtonEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    assimilateButtonSelector
  ) as HTMLButtonElement | null
}

export function skipButtonEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    skipButtonSelector
  ) as HTMLInputElement | null
}

export function removeFromRecallButtonEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    removeFromRecallButtonSelector
  ) as HTMLInputElement | null
}

export function returnToSequenceButtonEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    returnToSequenceButtonSelector
  ) as HTMLInputElement | null
}

export function assimilateOptionsCaretEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    assimilateOptionsCaretSelector
  ) as HTMLElement | null
}

export function assimilateAsCommissionedButtonEl() {
  return document.body.querySelector(
    '[data-test="assimilate-as-commissioned"]'
  ) as HTMLButtonElement | null
}

export function rememberSpellingButtonEl() {
  return document.body.querySelector(
    '[data-test="remember-spelling"]'
  ) as HTMLButtonElement | null
}

export async function clickAssimilate(wrapper: VueWrapper) {
  assimilateButtonEl(wrapper)!.click()
  await flushPromises()
}

export async function clickRemoveFromRecallAndConfirm(wrapper: VueWrapper) {
  removeFromRecallButtonEl(wrapper)!.click()
  usePopups().popups.done(true)
  await flushPromises()
}

export async function clickSkipAndConfirm(wrapper: VueWrapper) {
  skipButtonEl(wrapper)!.click()
  usePopups().popups.done(true)
  await flushPromises()
}

export async function clickReturnToSequence(wrapper: VueWrapper) {
  returnToSequenceButtonEl(wrapper)!.click()
  await flushPromises()
}

export async function openAssimilateOptions(wrapper: VueWrapper) {
  assimilateOptionsCaretEl(wrapper)!.click()
  await flushPromises()
}

export async function clickAssimilateAsCommissioned(wrapper: VueWrapper) {
  await openAssimilateOptions(wrapper)
  assimilateAsCommissionedButtonEl()!.click()
  await flushPromises()
}

export async function clickRememberSpelling(wrapper: VueWrapper) {
  await openAssimilateOptions(wrapper)
  rememberSpellingButtonEl()!.click()
  await flushPromises()
}
