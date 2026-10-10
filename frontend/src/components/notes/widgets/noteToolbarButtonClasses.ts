export const toolbarGhostBtnClass = "daisy-btn daisy-btn-ghost daisy-btn-sm"

export const toolbarToggleBtnClass = (pressed: boolean) =>
  pressed
    ? "daisy-btn daisy-btn-sm daisy-btn-soft daisy-btn-primary shrink-0"
    : toolbarGhostBtnClass
