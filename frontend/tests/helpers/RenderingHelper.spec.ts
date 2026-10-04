import { describe, it, expect } from "vitest"
import { flushPromises } from "@vue/test-utils"
import { defineComponent, h } from "vue"
import { useRoute } from "vue-router"
import helper, { productionRouterAt } from "@tests/helpers"

const CurrentRouteName = defineComponent({
  setup() {
    const route = useRoute()
    return () => h("span", String(route.name))
  },
})

describe("withRouter", () => {
  it("lets an earlier test leave a router at the recall location", async () => {
    const router = await productionRouterAt({ name: "recall" })
    expect(router.currentRoute.value.name).toBe("recall")
  })

  it("starts at root whatever the earlier test left", async () => {
    const wrapper = helper.component(CurrentRouteName).withRouter().mount()
    await flushPromises()
    expect(wrapper.text()).toBe("root")
  })
})
