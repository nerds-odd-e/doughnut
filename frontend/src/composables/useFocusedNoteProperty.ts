import {
  propertyItemIndex,
  type PropertyItemFocus,
} from "@/utils/noteProperties"
import { noteRouteFamilyNoteId } from "@/routes/noteRouteFamily"
import {
  locationKeepingQuery,
  notePropertyKeyFromRoute,
  notePropertyItemFromRoute,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  computed,
  inject,
  nextTick,
  provide,
  ref,
  toValue,
  watch,
  type ComponentPublicInstance,
  type InjectionKey,
  type MaybeRefOrGetter,
} from "vue"
import { useRoute, useRouter } from "vue-router"

const focusedPropertyKeyKey: InjectionKey<
  MaybeRefOrGetter<string | undefined>
> = Symbol("focusedPropertyKey")

export function provideFocusedPropertyKey(
  propertyKey: MaybeRefOrGetter<string | undefined>
) {
  provide(focusedPropertyKeyKey, propertyKey)
}

function scrollPropertyRowIntoView(element: HTMLElement) {
  element.scrollIntoView({
    behavior: "smooth",
    block: "center",
  })
}

export function useFocusedNoteProperty(
  propertyKeys?: MaybeRefOrGetter<readonly string[]>
) {
  const route = useRoute()
  const router = useRouter()
  const providedPropertyKey = inject(focusedPropertyKeyKey, undefined)
  const propertyRowElements = new Map<string, HTMLElement>()
  // Off note routes (notebook/folder readme) there is no property location.
  const offNoteRoutePropertyKey = ref<string>()
  const focusedProperty = computed<
    { key: string; item?: PropertyItemFocus } | undefined
  >(() => {
    const key =
      providedPropertyKey !== undefined
        ? toValue(providedPropertyKey)
        : noteRouteFamilyNoteId(route) === undefined
          ? offNoteRoutePropertyKey.value
          : notePropertyKeyFromRoute(route)
    if (key === undefined) return
    return {
      key,
      item:
        providedPropertyKey === undefined
          ? notePropertyItemFromRoute(route)
          : undefined,
    }
  })
  const focusedPropertyKey = computed(() => focusedProperty.value?.key)
  const unresolvedPropertyKey = computed(() => {
    if (propertyKeys === undefined) {
      return
    }
    const key = focusedPropertyKey.value
    if (!key || toValue(propertyKeys).includes(key)) {
      return
    }
    return key
  })

  const isFocusedProperty = (propertyKey: string) =>
    focusedPropertyKey.value === propertyKey

  const togglePropertyPanel = (propertyKey: string) => {
    const closing = isFocusedProperty(propertyKey)
    const noteId = noteRouteFamilyNoteId(route)
    if (noteId === undefined) {
      offNoteRoutePropertyKey.value = closing ? undefined : propertyKey
      return
    }
    return router.replace(
      locationKeepingQuery(
        route,
        closing
          ? noteShowLocation(Number(noteId))
          : notePropertyLocation(Number(noteId), propertyKey)
      )
    )
  }

  const setPropertyRowRef = (
    propertyKey: string,
    element: Element | ComponentPublicInstance | null
  ) => {
    if (element instanceof HTMLElement) {
      propertyRowElements.set(propertyKey, element)
      if (focusedPropertyKey.value === propertyKey) {
        scrollPropertyRowIntoView(element)
      }
      return
    }
    propertyRowElements.delete(propertyKey)
  }

  watch(
    [
      focusedPropertyKey,
      () => (propertyKeys === undefined ? undefined : toValue(propertyKeys)),
    ],
    async ([key, keys]) => {
      if (!key || (keys !== undefined && !keys.includes(key))) return
      await nextTick()
      const element = propertyRowElements.get(key)
      if (element) scrollPropertyRowIntoView(element)
    },
    { immediate: true, flush: "post" }
  )

  const focusedItemIndex = (propertyKey: string, items: readonly string[]) => {
    const focus = focusedProperty.value
    return focus?.key === propertyKey && focus.item
      ? propertyItemIndex(items, focus.item)
      : -1
  }

  return {
    focusedProperty,
    focusedItemIndex,
    isFocusedProperty,
    setPropertyRowRef,
    togglePropertyPanel,
    unresolvedPropertyKey,
  }
}
