import ManagedApi from "@/managedApi/ManagedApi"
import { render } from "@testing-library/vue"
import { VueWrapper, mount } from "@vue/test-utils"
import { merge } from "lodash"
import { ComponentPublicInstance, DefineComponent } from "vue"
import { RouteLocationRaw } from "vue-router"
import createNoteStorage from "../../src/store/createNoteStorage"

interface NoteStorageProps {
  storageAccessor?: ReturnType<typeof createNoteStorage>
  [key: string]: unknown
}
class RenderingHelper {
  private comp

  private props = {}

  private route = {}

  private managedApi

  private global

  constructor(comp: DefineComponent, managedApi: ManagedApi) {
    this.comp = comp
    this.managedApi = managedApi
    this.global = {
      directives: {
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        focus() {
          // noop
        },
      },
      provide: {
        managedApi: this.managedApi,
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

  withMockRouterPush(push) {
    this.withGlobalMock({ $router: { push } })
    return this
  }

  withGlobalMock(mocks: Record<string, unknown>) {
    this.global = merge(this.global, { mocks })
    return this
  }

  currentRoute(route: RouteLocationRaw) {
    this.route = route
    return this
  }

  render() {
    return render(this.comp, this.options)
  }

  mount(
    options: Record<string, unknown> = {}
  ): VueWrapper<ComponentPublicInstance> {
    return mount(this.comp, { ...this.options, ...options })
  }

  private get options() {
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
