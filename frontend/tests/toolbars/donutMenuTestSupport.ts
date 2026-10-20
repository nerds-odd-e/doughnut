import { UserController } from "@generated/donut-backend-api/sdk.gen"
import DonutMenu from "@/components/toolbars/DonutMenu.vue"
import { useSignedInMenuData } from "@/composables/useSignedInMenuData"
import type { User } from "@generated/donut-backend-api"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, productionRouterAt } from "@tests/helpers"
import { resetRecallData } from "@tests/helpers/recallDataTestSupport"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, afterEach, expect, vi } from "vitest"
import { defineComponent, h, type PropType, toRef } from "vue"
import type { Router } from "vue-router"
import { defaultMenuData } from "./menuDataMocks"

export let router: Router
export let user: User

// Mounts the launcher beside the app-shell menu data, as DonutApp and GlobalBar do.
const DonutMenuInAppShell = defineComponent({
  props: { user: { type: Object as PropType<User>, required: false } },
  setup(props) {
    useSignedInMenuData(toRef(props, "user"))
    return () => h(DonutMenu, { user: props.user })
  },
})

// Pass null to mount for a signed-out visitor.
export const mountDonutMenu = (menuUser: User | null = user) =>
  helper
    .component(DonutMenuInAppShell)
    .withProps({ user: menuUser ?? undefined })
    .withRealRouter(router)
    .render()

export const launcher = () => screen.getByRole("button", { name: "Donut menu" })

export const pressLauncher = async () => {
  await fireEvent.click(launcher())
  await flushPromises()
}

export const openDonutMenu = async (menuUser?: User | null) => {
  const result = mountDonutMenu(menuUser)
  await flushPromises()
  await pressLauncher()
  return result
}

export const panelItemLabels = () =>
  Array.from(document.querySelectorAll(".donut-menu-panel .nav-item")).map(
    (el) => el.querySelector("[aria-label]")?.getAttribute("aria-label")
  )

export const expectPanelClosed = () =>
  expect(document.querySelector(".donut-menu-panel")).toBeNull()

export const navItemOf = (label: string) =>
  screen.getByLabelText(label).closest(".nav-item")

export function setupDonutMenuTests() {
  beforeEach(async () => {
    vi.clearAllMocks()
    router = await productionRouterAt({ name: "root" })
    mockSdkService(UserController, "getMenuData", defaultMenuData)
    resetRecallData()
    user = makeMe.aUser.please()
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.restoreAllMocks()
  })
}
