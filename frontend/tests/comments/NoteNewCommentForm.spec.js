/**
 * @jest-environment jsdom
 */
import NoteCommentForm from "@/components/comment/NoteCommentForm.vue";
import store from "../../src/store/index.js";
import { renderWithStoreAndMockRoute } from "../helpers";


  describe('notes comment form', () => {
    it('render comment form', () => {
      const { wrapper } = renderWithStoreAndMockRoute(
        store,
        NoteCommentForm
      );
      expect(wrapper.container.querySelector('input')).toBeTruthy()
    })
  })