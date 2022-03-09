import { merge } from "lodash";
import { mount } from "@vue/test-utils";
import { render } from "@testing-library/vue";
import { createTestingPinia, TestingPinia } from "@pinia/testing";
import createPiniaStore from '../src/store/createPiniaStore';

type Options = {};

const withMockRoute = (
  comp: any,
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
          focus() {}
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
) => {
  return withMockRoute(comp, options, currentRoute, mount);
};

const renderWithMockRoute = (
  comp: any,
  options: Options = {},
  currentRoute: any
) => {
  return withMockRoute(comp, options, currentRoute, render);
};

const renderWithStoreAndMockRoute = (
  store: any,
  comp: any,
  options: Options = {},
  currentRoute: any = undefined,
) => {
  return withMockRoute(
    comp,
    merge(options, {
      global: {
        plugins: [store],
      },
    }),
    currentRoute,
    render
  );
};


const mountWithStoreAndMockRoute = (
  store: any,
  comp: any,
  options: Options = {},
  currentRoute: any,
) => {
  return withMockRoute(
    comp,
    merge(options, {
      global: {
        plugins: [store],
      },
    }),
    currentRoute,
    mount
  );
};

type PiniaStore = ReturnType<typeof createPiniaStore>

class StoredComponentTestHelper {
  private piniaInstance?: TestingPinia
  private piniaStore?: PiniaStore

  get pinia() {
    return this.piniaInstance || (this.piniaInstance = createTestingPinia())
  }

  get store(): PiniaStore {
    return this.piniaStore || (this.piniaStore = createPiniaStore(this.pinia))
  }

  render(
    comp: any,
    options: Options = {},
    currentRoute: any = undefined,
  ) {
    return renderWithStoreAndMockRoute(this.pinia, comp, options, currentRoute);
  }

}

export { StoredComponentTestHelper, mountWithMockRoute, renderWithMockRoute, renderWithStoreAndMockRoute, mountWithStoreAndMockRoute };
