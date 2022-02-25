/**
 * @jest-environment jsdom
 */
 import { screen } from "@testing-library/vue";
 import { renderWithStoreAndMockRoute } from "../helpers";
 import store from "../../src/store/index.js";
 import NoteShowCommentButton from "@/components/notes/NoteShowCommentButton.vue";
 
describe('List of comment cards', () => {
  describe('List of comments', () =>{
    beforeAll(() => {
      store.state.currentUser = {id: 1}
    })

    it('Display 1 comment', () => {
      renderWithStoreAndMockRoute(store, NoteShowCommentButton, { props: { comments: [{user: {id: 1}, content:"this is a comment"}] } });
      expect(screen.getByText('this is a comment'));
    })

    it('Display 2 comments', () => {
      renderWithStoreAndMockRoute(store, NoteShowCommentButton, { props: { comments: [{user: {id: 1}, content:"this is a comment"}, {user: {id: 1}, content:"this is another one"}] } });
      expect(screen.getByText('this is a comment'));
      expect(screen.getByText('this is another one'));
    })
  })
})
