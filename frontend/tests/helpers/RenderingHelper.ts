import { merge } from "lodash";
import { mount } from "@vue/test-utils";
import { TestingPinia } from "@pinia/testing";
import { render } from "@testing-library/vue";
import { DefineComponent } from "vue";
import createPiniaStore from '../../src/store/createPiniaStore';

type Options = Record<string, unknown>;

const withMockRoute = <T>(
  comp: T,
  options: Options = {},
  currentRoute: any,
  func: (comp: any, options: Options) => any
) => {
  const mockRouter = {
    push: jest.fn(),
  };

  const wrapper = func(
    comp,
    merge(options, {
      global: {
        directives: {
          focus() { }
        },
        mocks: {
          $route: currentRoute,
          $router: mockRouter,
        },
        stubs: {
          "router-view": true,
          "router-link": {
            props: ["to"],
            template: `<a class="router-link" :to='JSON.stringify(to)'><slot/></a>`,
          },
        },
      },
    })
  );

  return { wrapper, mockRouter };
};

type PiniaStore = ReturnType<typeof createPiniaStore>

interface StoreHelper {
  pinia: TestingPinia
  store: PiniaStore
}

class RenderingHelper {
  private helper

  private comp

  private props = {}

  private route = {}

  private global = {}

  constructor(helper: StoreHelper, comp: DefineComponent) {
    this.helper = helper
    this.comp = comp
  }

  withProps(props: Options) {
    this.props = props
    return this
  }

  withGlobal(global: Options) {
    this.global = global
    return this
  }

  currentRoute(route: any) {
    this.route = route
    return this
  }

  render() {
    return withMockRoute(
      this.comp,
      this.options,
      this.route,
      render
    );
  }

  mount() {
    return withMockRoute(
      this.comp,
      this.options,
      this.route,
      mount
    );
  }

  private get options() {
    return {
        propsData: this.props,
        global: merge(this.global, {
          plugins: [this.helper.pinia],
        }),
      }
  }

}


export default RenderingHelper
export { PiniaStore }
