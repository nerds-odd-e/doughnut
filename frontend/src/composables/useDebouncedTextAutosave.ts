import { debounce } from "es-toolkit"
import { computed, onUnmounted, ref, watch, type Ref } from "vue"

/** Debounce for note body and container indexContent autosave. */
export const TEXT_AUTOSAVE_DEBOUNCE_MS = 1000

export type DebouncedTextAutosaveOptions = {
  /** External source of truth (e.g. prop); watched for navigation/sync. */
  externalValue: Ref<string | undefined> | (() => string | undefined)
  persist: (value: string) => Promise<void>
  normalize?: (value: string) => string
  beforePersist?: (lastSaved: string, newValue: string) => Promise<boolean>
  shouldFlushImmediately?: (
    previousNormalized: string,
    nextNormalized: string
  ) => boolean
  onError?: (errs: unknown) => void
  /** When true, cancel pending save on unmount instead of flushing. */
  cancelOnUnmount?: boolean
}

function readExternal(
  externalValue: DebouncedTextAutosaveOptions["externalValue"]
): string {
  const raw =
    typeof externalValue === "function" ? externalValue() : externalValue.value
  return raw ?? ""
}

/**
 * Debounced text autosave shared by note body editing and container indexContent.
 */
export function useDebouncedTextAutosave(
  options: DebouncedTextAutosaveOptions
) {
  const normalize = options.normalize ?? ((value: string) => value)
  const savedVersion = ref(0)
  const lastSavedValue = ref(readExternal(options.externalValue))
  const pendingSaveValues = new Set<string>()
  const localValue = ref(lastSavedValue.value)
  const version = ref(0)
  const errors = ref({} as Record<string, string>)

  const hasUnsavedChanges = (): boolean =>
    normalize(localValue.value ?? "") !== normalize(lastSavedValue.value ?? "")

  const isDirty = computed(() => hasUnsavedChanges())

  const persistInner = async (newValue: string, nextVersion: number) => {
    if (options.beforePersist) {
      const proceed = await options.beforePersist(
        lastSavedValue.value ?? "",
        newValue
      )
      if (!proceed) {
        return
      }
    }
    pendingSaveValues.add(newValue)
    try {
      await options.persist(newValue)
      savedVersion.value = nextVersion
      lastSavedValue.value = newValue
    } catch (errs: unknown) {
      options.onError?.(errs)
    } finally {
      pendingSaveValues.delete(newValue)
    }
  }

  const debouncedPersist = debounce(persistInner, TEXT_AUTOSAVE_DEBOUNCE_MS)

  const propose = (newValue: string) => {
    const normalizedNewValue = normalize(newValue)
    const normalizedLastSaved = normalize(lastSavedValue.value ?? "")
    const prevNormalized = normalize(localValue.value ?? "")

    errors.value = {}
    localValue.value = newValue

    if (normalizedNewValue === normalizedLastSaved) {
      return
    }

    debouncedPersist(normalizedNewValue, version.value + 1)
    version.value += 1
    if (options.shouldFlushImmediately?.(prevNormalized, normalizedNewValue)) {
      debouncedPersist.flush()
    }
  }

  const flush = () => {
    debouncedPersist.flush()
  }

  const cancel = () => {
    debouncedPersist.cancel()
  }

  const discardDraft = () => {
    errors.value = {}
    localValue.value = lastSavedValue.value ?? ""
    version.value = savedVersion.value
  }

  const replaceFromExternal = (newValue: string) => {
    cancel()
    version.value = savedVersion.value
    localValue.value = newValue
    lastSavedValue.value = newValue
  }

  const syncFromExternal = (newValue: string | undefined) => {
    const valueToSet = newValue ?? ""
    if (version.value !== savedVersion.value) {
      if (valueToSet !== "" && pendingSaveValues.has(valueToSet)) {
        return
      }
      const normalizedCurrentValue = localValue.value ?? ""
      const normalizedLastSaved = lastSavedValue.value ?? ""
      if (
        valueToSet !== normalizedCurrentValue &&
        valueToSet !== normalizedLastSaved
      ) {
        replaceFromExternal(valueToSet)
      }
      return
    }
    localValue.value = valueToSet
    lastSavedValue.value = valueToSet
  }

  watch(
    () => readExternal(options.externalValue),
    (next) => {
      syncFromExternal(next)
    }
  )

  onUnmounted(() => {
    if (options.cancelOnUnmount) {
      cancel()
    } else {
      flush()
    }
  })

  return {
    localValue,
    errors,
    isDirty,
    hasUnsavedChanges,
    propose,
    flush,
    cancel,
    discardDraft,
    markSaved: (value: string) => {
      lastSavedValue.value = value
      savedVersion.value = version.value
    },
  }
}
