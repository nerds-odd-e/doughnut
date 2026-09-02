import { flushPromises, type VueWrapper } from "@vue/test-utils"
import usePopups from "@/components/commons/Popups/usePopups"

export const assimilateButtonSelector =
  '[data-test="assimilate-UNDERSTANDING"]' as const
export const assimilateAsCommissionedButtonSelector =
  '[data-test="assimilate-COMMISSIONED"]' as const
export const rememberSpellingButtonSelector =
  '[data-test="assimilate-SPELLING"]' as const
export const skipButtonSelector = '[data-test="skip"]' as const
export const returnToSequenceButtonSelector =
  '[data-test="return-to-sequence"]' as const
export const commissionedStatusSelector =
  '[data-test="assimilation-status-COMMISSIONED"]' as const
export const spellingStatusSelector =
  '[data-test="assimilation-status-SPELLING"]' as const
export const understandingStatusSelector =
  '[data-test="assimilation-status-UNDERSTANDING"]' as const

export function assimilateButtonEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    assimilateButtonSelector
  ) as HTMLInputElement | null
}

export function skipButtonEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    skipButtonSelector
  ) as HTMLInputElement | null
}

export function returnToSequenceButtonEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    returnToSequenceButtonSelector
  ) as HTMLInputElement | null
}

export function assimilateAsCommissionedButtonEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    assimilateAsCommissionedButtonSelector
  ) as HTMLInputElement | null
}

export function rememberSpellingButtonEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    rememberSpellingButtonSelector
  ) as HTMLInputElement | null
}

export function commissionedStatusEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    commissionedStatusSelector
  ) as HTMLAnchorElement | null
}

export function spellingStatusEl(wrapper: VueWrapper) {
  return wrapper.element.querySelector(
    spellingStatusSelector
  ) as HTMLAnchorElement | null
}

export async function clickAssimilate(wrapper: VueWrapper) {
  assimilateButtonEl(wrapper)!.click()
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

export async function clickAssimilateAsCommissioned(wrapper: VueWrapper) {
  assimilateAsCommissionedButtonEl(wrapper)!.click()
  await flushPromises()
}

export async function clickRememberSpelling(wrapper: VueWrapper) {
  rememberSpellingButtonEl(wrapper)!.click()
  await flushPromises()
}
