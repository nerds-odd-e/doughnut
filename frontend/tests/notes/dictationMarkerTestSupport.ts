import { expect } from "vitest"

/** The pending marker of a voice-input session, when one is drawn. */
export const dictationMarker = () =>
  document.querySelector<HTMLElement>('[data-testid="dictation-marker"]')

/** The marker is drawn at `place`, a measured text position in viewport coordinates. */
export function expectDictationMarkerAt(place: {
  left: number
  top: number
  height: number
}) {
  const drawn = dictationMarker()!.getBoundingClientRect()
  expect(place.height).toBeGreaterThan(0)
  expect(drawn.left).toBeCloseTo(place.left, 1)
  expect(drawn.top).toBeCloseTo(place.top, 1)
  expect(drawn.height).toBeCloseTo(place.height, 1)
}
