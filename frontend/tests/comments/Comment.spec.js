/**
 * @jest-environment jsdom
 */
 import CommentCard from "@/components/comment/Comment.vue";
 import { screen } from "@testing-library/vue";
 import store from '../../src/store/index.js'
 import { renderWithStoreAndMockRoute } from "../helpers";
 
describe('Testing Comment Card', () => {
  describe('should render comment card with the correct text in content prop', () =>{
    beforeAll(() => {
      store.state.currentUser = {id: 1}
    })

    it('should display string in content prop', () => {
      renderWithStoreAndMockRoute(store, CommentCard, { props: { comment: {user: {id: 1}, content: "this is a comment"} } });
      expect(screen.getByText('this is a comment'));
    })

    it('should not display string not in content prop', () => {
      renderWithStoreAndMockRoute(store, CommentCard, { props: { comment: {user: {id: 1}, content: "this is another comment" } } });
      expect(screen.queryByText('this is a comment')).toBe(null);
    })
  })   
})
