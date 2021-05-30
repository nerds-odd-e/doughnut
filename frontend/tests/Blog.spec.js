import Blog from '@/components/Blog.vue';
import { render } from '@testing-library/vue';

beforeEach(() => {
  window.alert = jest.fn();
  fetch.resetMocks();
});

const stubBlogPostsList = [
  {
    title: 'My first blog post',
    author: 'Lv Yi',
    createdDatetime: '30 May 2021',
    description: 'My draft initial blog post'
  }
];

describe('Blog Component unit tests: ', () => {
  test('fetchBlogPosts API to be called ONCE with article rendered', async () => {
    fetch.mockResponseOnce(JSON.stringify(stubBlogPostsList));

    const { findByText } = render(Blog);

    expect(fetch).toHaveBeenCalledTimes(1);
    const blogTitleElement = await findByText(/My first blog post/);
    const authorElement = await findByText(/Lv Yi/);
    const createdDateTimeElement = await findByText(/30 May 2021/);
    const descriptionElement = await findByText(/My draft initial blog post/);
    window.alert.mockClear();
  });
});
