import type { AnchoredDictationTarget } from "@/models/audio/dictationTarget"

const DOT =
  '<span class="size-[0.15em] rounded-full bg-current animate-pulse motion-reduce:animate-none"></span>'

/**
 * Lays an animated ellipsis over `host`, right after the target's anchor, from
 * now until the session ends; it follows each insertion. `host` is a
 * positioned element around the text. The marker is drawn, not written: it
 * holds no characters, so it is never part of the text.
 */
export function withDictationMarker(
  target: AnchoredDictationTarget,
  host: HTMLElement
): AnchoredDictationTarget {
  const marker = document.createElement("span")
  marker.setAttribute("aria-hidden", "true")
  marker.dataset.testid = "dictation-marker"
  marker.className =
    "absolute z-10 flex items-end gap-[0.1em] px-[0.1em] pb-[0.28em] rounded bg-base-100 pointer-events-none select-none"
  marker.innerHTML = DOT + DOT + DOT
  marker.children[1]!.classList.add("[animation-delay:300ms]")
  marker.children[2]!.classList.add("[animation-delay:600ms]")

  const place = () => {
    const anchor = target.anchorRect()
    const origin = host.getBoundingClientRect()
    Object.assign(marker.style, {
      left: `${anchor.left - origin.left - host.clientLeft}px`,
      top: `${anchor.top - origin.top - host.clientTop}px`,
      height: `${anchor.height}px`,
      fontSize: `${anchor.height}px`,
    })
  }
  host.appendChild(marker)
  place()
  // The text wraps again when the width changes.
  const hostResized = new ResizeObserver(place)
  hostResized.observe(host)

  return {
    ...target,
    insert: (segments) => {
      target.insert(segments)
      place()
    },
    end: (placeCaret) => {
      hostResized.disconnect()
      marker.remove()
      target.end(placeCaret)
    },
  }
}
