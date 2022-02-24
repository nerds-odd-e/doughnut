/**
 * @jest-environment jsdom
 */
 import { screen, render } from "@testing-library/vue";
 import NoteShowCommentButton from "@/components/notes/NoteShowCommentButton.vue";
 
describe('List of comment cards', () => {
  describe('List of comments', () =>{
    it('Display 1 comment', () => {
      render(NoteShowCommentButton, { props: { comments: [{content:"this is a comment"}] } });
      expect(screen.getByText('this is a comment'));
    })
  })   
})
