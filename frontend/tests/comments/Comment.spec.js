/**
 * @jest-environment jsdom
 */
 import CommentCard from "@/components/comment/Comment.vue";
 import { mount } from "@vue/test-utils";
 
describe('Testing Comment Card', () => {
  describe('should render comment card with the correct text in content prop', () =>{
    it('should display string in content prop', () => {
      const wrapper = mount(CommentCard, { propsData: { content: "this is a comment" } });
      expect(wrapper.text().includes('this is a comment')).toBe(true);
    })

    it('should not display string not in content prop', () => {
      const wrapper = mount(CommentCard, { propsData: { content: "this is another comment" } });
      expect(wrapper.text().includes('this is a comment')).toBe(false);
    })
  })   
})
