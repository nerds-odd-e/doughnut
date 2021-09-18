import { merge } from "lodash";
import { Component } from "vue";
import { mount } from "@vue/test-utils";
import { render } from "@testing-library/vue";
import store from "../src/store/index.js";
import { staticInfo } from "./notes/fixtures-basic.js";

type Options = {};

const withMockRoute = (
  comp: Component,
  options: Options = {},
  currentRoute: any,
  func: (comp: Component, options: Options) => any
) => {
  const mockRouter = {
    push: jest.fn(),
  };

  const wrapper = func(
    comp,
    merge(options, {
      global: {
        mocks: {
          $staticInfo: staticInfo,
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
  comp: Component,
  options: Options = {},
  currentRoute: any
) => {
  return withMockRoute(comp, options, currentRoute, mount);
};

const renderWithMockRoute = (
  comp: Component,
  options: Options = {},
  currentRoute: any
) => {
  return withMockRoute(comp, options, currentRoute, render);
};

const renderWithStoreAndMockRoute = (
  comp: Component,
  options: Options = {},
  currentRoute: any,
  func: (store: any) => void
) => {
  func(store);
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

export { mountWithMockRoute, renderWithMockRoute, renderWithStoreAndMockRoute };
