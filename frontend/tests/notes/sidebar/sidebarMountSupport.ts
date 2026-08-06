import Sidebar from "@/components/notes/Sidebar.vue"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { type VueWrapper, DOMWrapper, flushPromises } from "@vue/test-utils"
import { computed, defineComponent } from "vue"
import { useRouter, type RouteLocationRaw } from "vue-router"
import { expect } from "vitest"
import type { SidebarTreeFixtures } from "./sidebarFolderListingSupport"

/** SPA navigation only (no document navigation); matches real RouterLink enough for sidebar tests. */
const sidebarRouterLinkStub = defineComponent({
  name: "SidebarRouterLinkStub",
  props: {
    to: { type: [String, Object], required: true },
  },
  inheritAttrs: true,
  setup(props) {
    const router = useRouter()
    const href = computed(() => {
      try {
        return router.resolve(props.to as RouteLocationRaw).href
      } catch {
        return "#"
      }
    })
    async function onClick() {
      await router.push(props.to as RouteLocationRaw)
    }
    return { href, onClick }
  },
  template: `<a class="router-link" :href="href" @click.prevent="onClick"><slot /></a>`,
})

export function isBefore(node1: Node, node2: Node) {
  return !!(
    // eslint-disable-next-line no-bitwise
    (node1.compareDocumentPosition(node2) & Node.DOCUMENT_POSITION_FOLLOWING)
  )
}

export function findSidebarItem(
  wrapper: VueWrapper<unknown>,
  text: string
): DOMWrapper<Element> | undefined {
  const inner = wrapper
    .findAll(".title-text")
    .find((el) => el.text().includes(text))
  const li = inner?.element.closest("li")
  return li != null ? new DOMWrapper(li) : undefined
}

export function rootRowLabels(w: VueWrapper<unknown>): string[] {
  const rootUl = w.get("ul.sidebar-tree-list")
  return Array.from(rootUl.element.children).map((li) => {
    const folderText = li
      .querySelector(".sidebar-folder-label")
      ?.textContent?.trim()
    const noteText = li.querySelector(".title-text")?.textContent?.trim()
    return folderText
      ? `folder:${folderText}`
      : noteText
        ? `note:${noteText}`
        : "?"
  })
}

export function sidebarShowsActiveItem(
  wrapper: VueWrapper<unknown>,
  noteTitle: string
): boolean {
  return (
    findSidebarItem(wrapper, noteTitle)?.element.classList.contains(
      "active-item"
    ) ?? false
  )
}

export type SidebarTestHelper = typeof import("@tests/helpers").default

/** Mount first-generation sidebar and assert the active note row is in the DOM. */
export async function mountSidebarFirstGenReady(
  h: SidebarTestHelper,
  fixtures: SidebarTreeFixtures
) {
  const wrapper = mountSidebar(h, fixtures.firstGeneration)
  await flushPromises()
  const title = fixtures.firstGeneration.note.noteTopology.title
  const item = findSidebarItem(wrapper, title)
  expect(item, `sidebar note row "${title}"`).toBeDefined()
  expect(item!.exists()).toBe(true)
  return wrapper
}

export function mountSidebar(
  h: SidebarTestHelper,
  active: NoteRealm,
  notebookReadonly?: boolean
) {
  return h
    .component(Sidebar)
    .withRouter()
    .withProps({
      activeNoteRealm: active,
      notebookId: active.notebookRealm.notebook.id,
      notebookReadonly,
      breadcrumbFolders: active.ancestorFolders ?? [],
    })
    .mount({
      attachTo: document.body,
      global: {
        stubs: {
          RouterLink: sidebarRouterLinkStub,
          "router-link": sidebarRouterLinkStub,
        },
      },
    })
}

export function mountSidebarSignedIn(
  h: SidebarTestHelper,
  active: NoteRealm | undefined,
  notebookId: number,
  notebookReadonly?: boolean
) {
  return h
    .component(Sidebar)
    .withRouter()
    .withCurrentUser(makeMe.aUser.please())
    .withProps({
      activeNoteRealm: active,
      notebookId,
      notebookReadonly,
      breadcrumbFolders: active?.ancestorFolders ?? [],
    })
    .mount({
      attachTo: document.body,
      global: {
        stubs: {
          RouterLink: sidebarRouterLinkStub,
          "router-link": sidebarRouterLinkStub,
        },
      },
    })
}
