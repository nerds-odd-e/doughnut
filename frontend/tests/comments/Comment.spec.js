/**
 * @jest-environment jsdom
 */
 import CommentCard from "@/components/comment/Comment.vue";
 import { screen, render } from "@testing-library/vue";
 
describe('Testing Comment Card', () => {
  describe('should render comment card with the correct text in content prop', () =>{
    it('should display string in content prop', () => {
      render(CommentCard, { props: { content: "this is a comment" } });
      expect(screen.getByText('this is a comment'));
    })

    it('should not display string not in content prop', () => {
      render(CommentCard, { props: { content: "this is another comment" } });
      expect(screen.queryByText('this is a comment')).toBe(null);
    })
  })   
})
