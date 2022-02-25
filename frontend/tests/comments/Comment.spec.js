/**
 * @jest-environment jsdom
 */
import CommentCard from "@/components/comment/Comment.vue";
import { screen } from "@testing-library/vue";
import store from '../../src/store/index.js'
import makeMe from "../fixtures/makeMe.ts";
import { renderWithStoreAndMockRoute } from "../helpers";

describe('Testing Comment Card', () => {
  describe('should render comment card with the correct text in content prop', () => {
    const userId = 1
    const content1 = 'this is a comment'

    beforeAll(() => {
      store.state.currentUser = { id: userId }
    })

    it('should display string in content prop', () => {
      renderWithStoreAndMockRoute(store, CommentCard, { props: { comment: makeMe.aComment.fromUser(userId).please() } });
      expect(screen.getByText(content1));
    })

    it('should not display string not in content prop', () => {
      renderWithStoreAndMockRoute(store, CommentCard, { props: { comment: makeMe.aComment.fromUser(userId).content('this is another comment').please() } });
      expect(screen.queryByText(content1)).toBe(null);
    })
  })
})
