import type { Notebook } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookPageWithNotebookSidebarLayout from "@tests/fixtures/NotebookPageWithNotebookSidebarLayout.vue"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import { createMemoryHistory, createRouter, type Router } from "vue-router"

export const notebookPageRouter = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: "/", redirect: "/notebooks/0" },
    {
      path: "/notebooks/:notebookId",
      name: "notebookPage",
      component: { template: "<div />" },
      props: true,
    },
  ],
})

export type MountNotebookPageOptions = {
  router?: Router
  indexContent?: string
}

export function mockNotebookGet(
  notebook: Notebook,
  options: { indexContent?: string } = {}
) {
  return mockSdkService(NotebookController, "get", {
    notebook,
    hasAttachedBook: false,
    readonly: false,
    ...(options.indexContent !== undefined
      ? { indexContent: options.indexContent }
      : {}),
  })
}

export function mountNotebookPage(
  notebook: Notebook,
  options: MountNotebookPageOptions = {}
) {
  const router = options.router ?? notebookPageRouter
  mockNotebookGet(notebook, { indexContent: options.indexContent })
  const wrapper = helper
    .component(NotebookPageWithNotebookSidebarLayout)
    .withCleanStorage()
    .withRouter(router)
    .withCurrentUser(makeMe.aUser.please())
    .mount()
  return { wrapper, router, notebook }
}

export async function mountNotebookPageReady(
  notebook: Notebook,
  options: MountNotebookPageOptions = {}
) {
  const mounted = mountNotebookPage(notebook, options)
  await navigateToNotebookPage(mounted.router, notebook.id)
  return mounted
}

export async function navigateToNotebookPage(
  router: Router,
  notebookId: number
) {
  await router.push({
    name: "notebookPage",
    params: { notebookId: String(notebookId) },
  })
  await flushPromises()
}

export function notebookIndexEditorEl(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="notebook-index-editor"]')
}

export function notebookIndexSaveEl(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="notebook-index-save"]')
}

export async function setNotebookIndexDraft(
  wrapper: VueWrapper,
  content: string
) {
  wrapper
    .findComponent({ name: "RichMarkdownEditor" })
    .vm.$emit("update:modelValue", content)
  await flushPromises()
}

export async function clickNotebookIndexSave(wrapper: VueWrapper) {
  await notebookIndexSaveEl(wrapper).trigger("click")
  await flushPromises()
}

export function hideSidebarButtonEl() {
  return document.body.querySelector(
    '[title="Hide sidebar"]'
  ) as HTMLElement | null
}
