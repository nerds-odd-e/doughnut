import ManagedApi from "@/managedApi/ManagedApi"
import { render } from "@testing-library/vue"
import { mount } from "@vue/test-utils"
import { merge } from "es-toolkit"
import { ref, type DefineComponent } from "vue"
import type { RouteLocationRaw } from "vue-router"
import { createRouter, createWebHistory } from "vue-router"
import createNoteStorage from "@/store/createNoteStorage"
import routes from "@/routes/routes"
import type { User } from "@generated/backend"

interface NoteStorageProps {
  storageAccessor?: ReturnType<typeof createNoteStorage>
  [key: string]: unknown
}
class RenderingHelper<T = DefineComponent> {
  private comp: T
  private props = {}

  private route = {}

  private managedApi

  private global

  constructor(comp: T, managedApi: ManagedApi) {
    this.comp = comp
    this.managedApi = managedApi
    this.global = {
      plugins: [],
      directives: {
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        focus() {
          // noop
        },
      },
      provide: {
        managedApi: this.managedApi,
        currentUser: ref<User | undefined>(),
      },
      stubs: {
        "router-view": true,
        "router-link": {
          props: ["to"],
          template: `<a class="router-link" :to='JSON.stringify(to)'><slot/></a>`,
        },
      },
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  withStorageProps(props: Partial<NoteStorageProps>) {
    return this.withProps({
      storageAccessor: createNoteStorage(this.managedApi),
      ...props,
    })
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  withProps(props: NoteStorageProps) {
    this.props = props
    return this
  }

  withRouter(routerParam?: ReturnType<typeof createRouter>) {
    const router =
      routerParam ??
      createRouter({
        history: createWebHistory(),
        routes,
      })
    this.withPlugin(router)
    return this
  }

  withCurrentUser(user: User) {
    this.global.provide.currentUser = ref(user)
    return this
  }

  withPlugin(plugin: unknown) {
    this.global.plugins = [...this.global.plugins, plugin]
    return this
  }

  currentRoute(route: RouteLocationRaw) {
    this.route = route
    return this
  }

  render() {
    return render(this.comp, this.options)
  }

  mount(options: Record<string, unknown> = {}) {
    return mount<T>(this.comp, {
      ...this.options,
      ...options,
    })
  }

  private get options(): Record<string, unknown> {
    return {
      propsData: this.props,
      global: merge(this.global, {
        mocks: {
          $route: this.route,
        },
      }),
    }
  }
}

export default RenderingHelper
