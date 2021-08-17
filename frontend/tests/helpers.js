import { mount } from '@vue/test-utils';
import { merge } from "lodash"

const mountWithMockRoute = (comp, options, currentRoute) => {
    const mockRoute = currentRoute

    const mockRouter = {
    push: jest.fn()
    }

    const wrapper = mount(
        comp,
        merge(
            options,
            {
            global: {
                mocks: {
                    $staticInfo: { colors: {}},
                    $route: mockRoute,
                    $router: mockRouter
                },
                stubs: {'router-view': true, 'router-link': {props: ['to'], template: `<a class="router-link" :to='JSON.stringify(to)'><slot/></a>`}}
            },
            }
        )
    );

    return { wrapper, mockRouter }
}

export { mountWithMockRoute }