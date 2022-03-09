import { merge } from "lodash";
import { mount } from "@vue/test-utils";
import { createTestingPinia, TestingPinia } from "@pinia/testing";
import { render } from "@testing-library/vue";
import { DefineComponent } from "vue";
import createPiniaStore from '../../src/store/createPiniaStore';
import RenderingHelper, { PiniaStore } from "./RenderingHelper";

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

class StoredComponentTestHelper {
  private piniaInstance?: TestingPinia

  private piniaStore?: PiniaStore

  get pinia() {
    return this.piniaInstance || (this.piniaInstance = createTestingPinia())
  }

  get store(): PiniaStore {
    return this.piniaStore || (this.piniaStore = createPiniaStore(this.pinia))
  }

  component(comp: DefineComponent) {
    return new RenderingHelper(this, comp)
  }

}

export { StoredComponentTestHelper, mountWithMockRoute, renderWithMockRoute, renderWithStoreAndMockRoute, mountWithStoreAndMockRoute };
