/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import { renderWithStoreAndMockRoute } from "../helpers";
import store from "../../src/store/index.js";
import NoteShowCommentButton from "@/components/notes/NoteShowCommentButton.vue";
import makeMe from "../fixtures/makeMe.ts";

describe('List of comment cards', () => {
  describe('List of comments', () => {
    const userId = 1
    const content1 = 'this is a comment'
    const content2 = 'this is another comment'
    const comment1 = makeMe.aComment.fromUser(userId).please()

    beforeAll(() => {
      store.state.currentUser = { id: userId }
    })

    it('Display 1 comment', () => {
      renderWithStoreAndMockRoute(store, NoteShowCommentButton, { props: { comments: [comment1] } });
      expect(screen.getByText(content1));
    })

    it('Display 2 comments', () => {
      renderWithStoreAndMockRoute(store, NoteShowCommentButton, { props: { comments: [comment1, makeMe.aComment.fromUser(userId).content(content2).please()] } });
      expect(screen.getByText(content1));
      expect(screen.getByText(content2));
    })
  })
})
