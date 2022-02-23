/**
 * @jest-environment jsdom
 */
import { render, screen } from '@testing-library/vue'
import NoteCommentForm from "@/components/comment/NoteCommentForm.vue";


  describe('notes comment form', () => {
    it.skip('render comment form', () => {
      render(NoteCommentForm);
      expect(screen.getByPlaceholderText('Add a comment')).toBeTruthy();
    })
  })