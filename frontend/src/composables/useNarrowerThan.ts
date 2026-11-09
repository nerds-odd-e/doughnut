import { onMounted, onUnmounted, ref, type Ref } from "vue"

/**
 * Whether `element` is narrower than `widthPx`, following its resizes.
 * Measured on mount so the first paint already uses it. A resize applies on
 * the next frame, so a layout change made in response cannot re-enter the
 * same resize notification.
 */
export function useNarrowerThan(
  element: Readonly<Ref<HTMLElement | null>>,
  widthPx: number
): Readonly<Ref<boolean>> {
  const narrower = ref(false)
  const measure = () => {
    narrower.value = element.value!.getBoundingClientRect().width < widthPx
  }
  let frame = 0
  const observer = new ResizeObserver(() => {
    cancelAnimationFrame(frame)
    frame = requestAnimationFrame(measure)
  })
  onMounted(() => {
    measure()
    observer.observe(element.value!)
  })
  onUnmounted(() => {
    cancelAnimationFrame(frame)
    observer.disconnect()
  })
  return narrower
}
