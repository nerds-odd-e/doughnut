/** DaisyUI classes for an outline icon button appended to a field control in a `daisy-join`. */
export const FIELD_JOIN_APPEND_BUTTON_CLASS =
  "daisy-btn daisy-btn-outline daisy-btn-neutral daisy-join-item donut-field-join-append-btn min-h-[2.75rem] shrink-0"

/** The same button as a toggle: highlighted while pressed. */
export const fieldJoinAppendToggleButtonClass = (pressed: boolean) =>
  pressed
    ? "daisy-btn daisy-join-item daisy-btn-soft daisy-btn-primary shrink-0"
    : FIELD_JOIN_APPEND_BUTTON_CLASS
