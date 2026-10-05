import routes from "@/routes/routes"
import { resetNoteStore } from "@/store/noteStore"
import type { User } from "@generated/donut-backend-api"
import { render } from "@testing-library/vue"
import { mount } from "@vue/test-utils"
import { merge } from "es-toolkit"
import { ref, type Component, type DefineComponent, type Ref } from "vue"
import type { RouteLocationRaw, Router } from "vue-router"
import { createRouter, createWebHistory } from "vue-router"

function productionRouter() {
  return createRouter({ history: createWebHistory(), routes })
}

/** A real router over the production routes, placed at `location`. */
export async function productionRouterAt(location: RouteLocationRaw) {
  const router = productionRouter()
  await router.push(location)
  return router
}

/**
 * Starts counting browser history entries the router adds from now on:
 * a push adds one, a replace adds none.
 */
export function countHistoryEntriesAdded(router: Router) {
  const position = () => router.options.history.state.position as number
  const before = position()
  return () => position() - before
}

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
    const stubs: Record<string, boolean | Component> = {
      "router-view": true,
      "router-link": {
        props: ["to"],
        template: `<a class="router-link" :to='JSON.stringify(to)' href="#"><slot/></a>`,
      },
    }
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
      stubs,
    }
  }

  withCleanStorage() {
    // Reset the singleton for each test
    resetNoteStore()
    return this
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  withProps(props: NoteStorageProps) {
    this.props = props
    return this
  }

  withRouter(routerParam?: ReturnType<typeof createRouter>) {
    this.withPlugin(routerParam ?? productionRouter())
    return this
  }

  withRealRouter(router: ReturnType<typeof createRouter>) {
    delete this.global.stubs["router-link"]
    return this.withRouter(router)
  }

  withCurrentUser(user: User) {
    this.global.provide.currentUser = ref(user)
    return this
  }

  withCurrentUserRef(userRef: Ref<User | undefined>) {
    this.global.provide.currentUser = userRef
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
