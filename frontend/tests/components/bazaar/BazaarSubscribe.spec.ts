import BazaarNotebookButtons from "@/components/bazaar/BazaarNotebookButtons.vue"
import SubscribeForm from "@/components/bazaar/SubscribeForm.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { afterEach, describe, expect, it } from "vitest"

describe("Bazaar subscribe", () => {
  afterEach(() => {
    document.body.innerHTML = ""
  })

  const notebook = makeMe.aNotebook.title("Shape").please()

  describe("BazaarNotebookButtons", () => {
    it("shows Subscribe CTA when memory tracking is enabled", () => {
      const wrapper = helper
        .component(BazaarNotebookButtons)
        .withProps({ notebook, loggedIn: true })
        .mount()

      expect(wrapper.find("button").attributes("title")).toBe("Subscribe")
      wrapper.unmount()
    })

    it("omits Subscribe CTA when notebook skips memory tracking entirely", () => {
      const skipped = makeMe.aNotebook.skipMemoryTrackingEntirely(true).please()
      const wrapper = helper
        .component(BazaarNotebookButtons)
        .withProps({ notebook: skipped, loggedIn: true })
        .mount()

      expect(wrapper.find("button").exists()).toBe(false)
      wrapper.unmount()
    })
  })

  describe("SubscribeForm", () => {
    it("labels dialog Subscribe and daily assimilation target field", () => {
      const wrapper = helper
        .component(SubscribeForm)
        .withProps({ notebook, loggedIn: true })
        .withRouter()
        .mount()

      expect(wrapper.find("h3").text()).toBe("Subscribe")
      expect(wrapper.find("label").text()).toBe("Daily assimilation target")
      wrapper.unmount()
    })
  })
})
