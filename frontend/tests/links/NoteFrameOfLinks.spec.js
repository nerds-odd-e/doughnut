/**
 * @jest-environment jsdom
 */
import _ from 'lodash';
import NoteFrameOfLinks from '@/components/links/NoteFrameOfLinks.vue';
import makeMe from '../fixtures/makeMe.ts';
import { mountWithMockRoute } from '../helpers';

describe('a link lists of a note', () => {
  it('link to upper level', async () => {
    const links = makeMe.links.of('using').count(2).please();
    const { wrapper } = mountWithMockRoute(NoteFrameOfLinks, {
      propsData: { links },
    });
    expect(wrapper.find('.parent-links').text()).toContain('a tool');
    expect(wrapper.findAll('.parent-links li').length).toEqual(2);
  });

  it('tags are grouped', async () => {
    const links = makeMe.links.of('tagged by').count(2).please();
    const { wrapper } = mountWithMockRoute(NoteFrameOfLinks, {
      propsData: { links },
    });
    expect(wrapper.findAll('.parent-links li').length).toEqual(1);
  });

  it('related, opposite, similar, confuse are grouped at top', async () => {
    const links = makeMe.links
      .of('confused with')
      .and.of('similar to')
      .please();
    const { wrapper } = mountWithMockRoute(NoteFrameOfLinks, {
      propsData: { links },
    });
    expect(wrapper.findAll('.parent-links li').length).toEqual(1);
    expect(wrapper.findAll('.parent-links li .link-multi').length).toEqual(2);
    expect(wrapper.findAll('.children-links li').length).toEqual(0);
  });

  it('taggings (reverse of tagged by) are grouped', async () => {
    const links = makeMe.links.of('tagged by').reverse.count(2).please();
    const { wrapper } = mountWithMockRoute(NoteFrameOfLinks, {
      propsData: { links },
    });
    expect(wrapper.findAll('.children-links li').length).toEqual(1);
  });
});
