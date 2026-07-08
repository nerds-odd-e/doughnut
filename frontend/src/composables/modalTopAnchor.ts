import {
  computed,
  inject,
  onMounted,
  onUnmounted,
  provide,
  ref,
  type ComputedRef,
  type InjectionKey,
} from "vue"

interface ModalTopAnchorContext {
  register: () => () => void
}

const modalTopAnchorKey: InjectionKey<ModalTopAnchorContext> =
  Symbol("modalTopAnchor")

export function provideModalTopAnchor(): ComputedRef<boolean> {
  const requestCount = ref(0)

  const context: ModalTopAnchorContext = {
    register: () => {
      requestCount.value++
      return () => {
        requestCount.value--
      }
    },
  }

  provide(modalTopAnchorKey, context)

  return computed(() => requestCount.value > 0)
}

/** Request a stable top edge for the enclosing modal (dynamic-height search results). */
export function useStableModalTop(): void {
  const context = inject(modalTopAnchorKey, null)

  if (!context) {
    return
  }

  let unregister: (() => void) | undefined

  onMounted(() => {
    unregister = context.register()
  })

  onUnmounted(() => {
    unregister?.()
  })
}
