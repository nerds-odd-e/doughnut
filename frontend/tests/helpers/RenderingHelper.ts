import { merge } from "lodash";
import { mount } from "@vue/test-utils";
import { render } from "@testing-library/vue";
import { DefineComponent } from "vue";

type Options = Record<string, unknown>;

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

  withProps(props: Options) {
    this.props = props
    return this
  }

  withMockRouterPush(push: jest.Mock) {
    this.withGlobal({
      mocks: { $router: {push} }
    })
    return this
  }

  withGlobal(global: Options) {
    this.global = merge(this.global, global)
    return this
  }

  currentRoute(route: any) {
    this.route = route
    return this
  }

  render() {
    return {
      wrapper: render( this.comp, this.options),
    };
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
