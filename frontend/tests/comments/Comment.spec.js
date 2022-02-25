/**
 * @jest-environment jsdom
 */
 import CommentCard from "@/components/comment/Comment.vue";
 import { screen } from "@testing-library/vue";
 import store from '../../src/store/index.js'
 import { renderWithStoreAndMockRoute } from "../helpers";

 const _generateComment = (userId, content) => {
   return {user: {id: userId}, content}
 }
 
describe('Testing Comment Card', () => {
  describe('should render comment card with the correct text in content prop', () =>{
    const userId = 1
    const content1 = 'this is a comment'

    beforeAll(() => {
      store.state.currentUser = {id: userId}
    })

    it('should display string in content prop', () => {
      renderWithStoreAndMockRoute(store, CommentCard, { props: { comment: _generateComment(userId, content1) } });
      expect(screen.getByText(content1));
    })

    it('should not display string not in content prop', () => {
      renderWithStoreAndMockRoute(store, CommentCard, { props: { comment: _generateComment(userId, "this is another comment") } });
      expect(screen.queryByText(content1)).toBe(null);
    })
  })   
})
