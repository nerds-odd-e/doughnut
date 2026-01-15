import { useStorageAccessor } from "@/composables/useStorageAccessor"
import routes from "@/routes/routes"
import createNoteStorage from "@/store/createNoteStorage"
import type { User } from "@generated/backend"
import { render } from "@testing-library/vue"
import { mount } from "@vue/test-utils"
import { merge } from "es-toolkit"
import { ref, type DefineComponent } from "vue"
import type { RouteLocationRaw } from "vue-router"
import { createRouter, createWebHistory } from "vue-router"

interface NoteStorageProps {
  [key: string]: unknown
}
class RenderingHelper<T = DefineComponent> {
  private comp: T
  private props = {}

  private route = {}

  private global

  constructor(comp: T) {
    this.comp = comp
    this.global = {
      plugins: [],
      directives: {
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        focus() {
          // noop
        },
      },
      provide: {
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

  withCleanStorage() {
    // Reset the singleton for each test
    const storageAccessor = useStorageAccessor()
    storageAccessor.value = createNoteStorage()
    return this
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
    const { global: existingGlobal = {} } = this.options
    const { global: newGlobal = {} } = options

    return mount<T>(this.comp, {
      ...this.options,
      ...options,
      global: merge(
        existingGlobal as Record<string, unknown>,
        newGlobal as Record<string, unknown>
      ),
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
