import NavBar from '@/components/NavBar.vue';
import { render } from '@testing-library/vue';

beforeEach(() => {
  window.alert = jest.fn();
  fetch.resetMocks();
});

const stubBlogPostsYearList = [{ year: 2018 }, { year: 2019 }];

describe('NavBar Component unit tests: ', () => {
  test('fetchBlogPostsYearList API to be called ONCE with years list rendered', async () => {
    fetch.mockResponseOnce(JSON.stringify(stubBlogPostsYearList));

    const { findByText } = render(NavBar);

    expect(fetch).toHaveBeenCalledTimes(1);
    const recentYearElement = await findByText(/Recent/);
    const year2018YearElement = await findByText(/2018/);
    const year2019YearElement = await findByText(/2019/);
    const oddeBlogBannerElement = await findByText(/Odd-e Blog/);
    const homeOfTheNerdsBannerElement = await findByText(/Home of the NERDs/);
    window.alert.mockClear();
  });
});
