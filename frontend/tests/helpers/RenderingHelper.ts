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

const mountWithMockRoute = (
  comp: any,
  options: Options = {},
  currentRoute: any
) => withMockRoute(comp, options, currentRoute, mount);

const renderWithMockRoute = (
  comp: any,
  options: Options = {},
  currentRoute: any
) => withMockRoute(comp, options, currentRoute, render);

const renderWithStoreAndMockRoute = <T>(
  store: any,
  comp: T,
  options: Options = {},
  currentRoute: any = undefined,
) => withMockRoute(
  comp,
  merge(options, {
    global: {
      plugins: [store],
    },
  }),
  currentRoute,
  render
);


const mountWithStoreAndMockRoute = (
  store: any,
  comp: any,
  options: Options = {},
  currentRoute: any,
) => withMockRoute(
  comp,
  merge(options, {
    global: {
      plugins: [store],
    },
  }),
  currentRoute,
  mount
);

type PiniaStore = ReturnType<typeof createPiniaStore>

interface StoreHelper {
  pinia: TestingPinia
  store: PiniaStore
}

class RenderingHelper {
  private helper

  private comp

  private props = {}

  constructor(helper: StoreHelper, comp: DefineComponent) {
    this.helper = helper
    this.comp = comp
  }

  withProps(props: Options) {
    this.props = props
    return this
  }

  render() {
    return renderWithStoreAndMockRoute(this.helper.pinia, this.comp, { propsData: this.props }, {});
  }

  mount() {
    return mountWithStoreAndMockRoute(this.helper.pinia, this.comp, { propsData: this.props }, {});
  }
}


export default RenderingHelper
export { PiniaStore }
