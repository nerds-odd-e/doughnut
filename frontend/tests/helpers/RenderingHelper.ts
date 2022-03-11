import { merge } from "lodash";
import { mount } from "@vue/test-utils";
import { render } from "@testing-library/vue";
import { App, DefineComponent } from "vue";
import { RouteLocationRaw } from "vue-router";

interface VuePlugin {
    install: (app: App) => void;
}

class RenderingHelper {
  private comp

  private props = {}

  private route = {}

  private global = {
    directives: {
      focus() { }
    },
    stubs: {
      "router-view": true,
      "router-link": {
        props: ["to"],
        template: `<a class="router-link" :to='JSON.stringify(to)'><slot/></a>`,
      },
    },
  }

  constructor(comp: DefineComponent) {
    this.comp = comp
  }

  withProps(props: any) {
    this.props = props
    return this
  }

  withMockRouterPush(push: jest.Mock) {
    this.withGlobalMock({ $router: {push} })
    return this
  }

  withGlobalPlugin(plugin: VuePlugin) {
    this.global = merge(this.global, {plugins: [plugin]})
    return this
  }

  withGlobalMock(mocks: Record<string, unknown>) {
    this.global = merge(this.global, {mocks})
    return this
  }

  currentRoute(route: RouteLocationRaw) {
    this.route = route
    return this
  }

  render() {
    return render(this.comp, this.options)
  }

  mount() {
    return mount(this.comp, this.options)
  }

  private get options() {
    return {
        propsData: this.props,
        global: merge(
          this.global,
          {
            mocks: {
              $route: this.route,
            },
          })
      }
  }
}

export default RenderingHelper
