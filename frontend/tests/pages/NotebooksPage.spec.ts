import NotebooksPage from "@/pages/NotebooksPage.vue"
import { describe, it, expect } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"

describe("Notebooks Page", () => {
  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please()

    const myNotebooksSpy = mockSdkService("myNotebooks", {
      notebooks: [notebook],
      subscriptions: [],
    })
    helper.component(NotebooksPage).withRouter().render()
    expect(myNotebooksSpy).toBeCalledTimes(1)
  })

  describe("notebook updates", () => {
    it("should update notebook in the list when notebook-updated event is emitted", async () => {
      const originalNotebook = {
        ...makeMe.aNotebook.please(),
        title: "Original Title",
      }
      const updatedNotebook = {
        ...originalNotebook,
        title: "Updated Title",
        notebookSettings: {
          ...originalNotebook.notebookSettings,
          numberOfQuestionsInAssessment: 10,
        },
      }

      mockSdkService("myNotebooks", {
        notebooks: [originalNotebook],
        subscriptions: [],
      })
      mockSdkService("updateNotebook", updatedNotebook)

      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()

      await flushPromises()

      const vm = wrapper.vm as unknown as {
        notebooks: (typeof originalNotebook)[] | undefined
      }

      // Verify initial state
      expect(vm.notebooks).toHaveLength(1)
      expect(vm.notebooks?.[0]?.title).toBe("Original Title")

      // Find and trigger the notebook-updated event
      const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
      notebookButtons.vm.$emit("notebook-updated", updatedNotebook)
      await flushPromises()

      // Verify the notebook was updated
      expect(vm.notebooks?.[0]?.title).toBe("Updated Title")
    })

    it("should handle notebook-updated event when notebooks array is populated", async () => {
      const notebook1 = {
        ...makeMe.aNotebook.please(),
        title: "Notebook 1",
      }
      const notebook2 = {
        ...makeMe.aNotebook.please(),
        title: "Notebook 2",
      }

      const updatedNotebook1 = {
        ...notebook1,
        title: "Updated Notebook 1",
      }

      mockSdkService("myNotebooks", {
        notebooks: [notebook1, notebook2],
        subscriptions: [],
      })

      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()

      await flushPromises()

      // Get component instance to check internal state
      const vm = wrapper.vm as unknown as {
        notebooks: (typeof notebook1)[] | undefined
      }

      // Verify initial state
      expect(vm.notebooks).toHaveLength(2)
      expect(vm.notebooks?.[0]?.title).toBe("Notebook 1")

      // Emit notebook-updated event
      const notebookButtons = wrapper.findAllComponents({
        name: "NotebookButtons",
      })[0]
      if (notebookButtons) {
        notebookButtons.vm.$emit("notebook-updated", updatedNotebook1)
      }
      await flushPromises()

      // Verify the notebook was updated
      expect(vm.notebooks).toHaveLength(2)
      expect(vm.notebooks?.[0]?.title).toBe("Updated Notebook 1")
      expect(vm.notebooks?.[1]?.title).toBe("Notebook 2")
    })

    it("should handle empty notebooks array gracefully", async () => {
      // Start with empty notebooks array
      mockSdkService("myNotebooks", {
        notebooks: [],
        subscriptions: [],
      })

      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()

      await flushPromises()

      const vm = wrapper.vm as unknown as {
        notebooks: (typeof makeMe.aNotebook)[] | undefined
      }

      // Verify empty state
      expect(vm.notebooks).toHaveLength(0)

      // NotebookButtons shouldn't exist when there are no notebooks
      const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
      expect(notebookButtons.exists()).toBe(false)
    })

    it("should handle event from NotebookButtons", async () => {
      const originalNotebook = {
        ...makeMe.aNotebook.please(),
        title: "Before Update",
      }
      const updatedNotebook = {
        ...originalNotebook,
        title: "After Update",
      }

      mockSdkService("myNotebooks", {
        notebooks: [originalNotebook],
        subscriptions: [],
      })

      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()

      await flushPromises()

      const vm = wrapper.vm as unknown as {
        notebooks: (typeof originalNotebook)[] | undefined
      }

      // Verify initial state
      expect(vm.notebooks?.[0]?.title).toBe("Before Update")

      // Simulate event from NotebookButtons
      const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
      notebookButtons.vm.$emit("notebook-updated", updatedNotebook)
      await flushPromises()

      // Verify the notebook was updated
      expect(vm.notebooks?.[0]?.title).toBe("After Update")
    })
  })
})
