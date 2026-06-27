import {
  computed,
  inject,
  provide,
  ref,
  type InjectionKey,
  type MaybeRefOrGetter,
  type Ref,
  toValue,
} from "vue"

export const noteShortcutScopeKey: InjectionKey<Ref<boolean>> =
  Symbol("noteShortcutScope")

export function provideNoteShortcutScope(
  active: MaybeRefOrGetter<boolean>
): Ref<boolean> {
  const scope = computed(() => toValue(active))
  provide(noteShortcutScopeKey, scope)
  return scope
}

export function useNoteShortcutScope(): Ref<boolean> {
  return inject(noteShortcutScopeKey, ref(true))
}
