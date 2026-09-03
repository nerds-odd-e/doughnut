import AssimilationModes from "@/components/recall/AssimilationModes.vue"
import helper from "@tests/helpers"

export function mountModes(props: Record<string, unknown>) {
  return helper.component(AssimilationModes).withProps(props).mount()
}

export function assimilateButton(
  wrapper: ReturnType<typeof mountModes>,
  mode: string
) {
  return wrapper.element.querySelector(
    `[data-test="assimilate-${mode}"]`
  ) as HTMLButtonElement | null
}

export function statusLink(
  wrapper: ReturnType<typeof mountModes>,
  mode: string
) {
  return wrapper.element.querySelector(
    `[data-test="assimilation-status-${mode}"]`
  ) as HTMLAnchorElement | null
}

export function actionSlot(
  wrapper: ReturnType<typeof mountModes>,
  mode: string
) {
  return wrapper.element.querySelector(
    `[data-test="assimilation-action-${mode}"]`
  ) as HTMLElement | null
}
