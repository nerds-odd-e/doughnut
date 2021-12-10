/**
 * @jest-environment jsdom
 */
import NoteContent from "@/components/notes/NoteContent.vue";
import makeMe from "../fixtures/makeMe";
import { mountWithStoreAndMockRoute } from '../helpers';
import store from '../../src/store';
import Languages from "../../src/models/languages";

describe("in place edit on description", () => {

  it("should display text field when one single click on description", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").description("Dummy Description").please();
    store.commit("loadNotes", [noteParent]);

    const { wrapper } = mountWithStoreAndMockRoute(
      store,
      NoteContent,
      { 
        props: { 
          note: noteParent,
          isInPlaceEditEnabled: true 
        } 
      },
    )

    await wrapper.find('#description-id').trigger('click');

    expect(wrapper.findAll('#description-form-id')).toHaveLength(1);
    expect(wrapper.findAll("#description-id")).toHaveLength(0);
  });

  it("should not display text field when one single click on description", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").description("Dummy Description").please();
    store.commit("loadNotes", [noteParent]);

    const { wrapper } = mountWithStoreAndMockRoute(
      store,
      NoteContent,
      { 
        props: { 
          note: noteParent,
          isInPlaceEditEnabled: false 
        } 
      },
    )

    await wrapper.find('#description-id').trigger('click');

    expect(wrapper.findAll('#description-form-id')).toHaveLength(0);
    expect(wrapper.findAll("#description-id")).toHaveLength(1);
  });

  it("should back to label when blur text field description", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").description("Dummy Description").please();
    store.commit("loadNotes", [noteParent]);

    const { wrapper } = mountWithStoreAndMockRoute(store, NoteContent, {
      props: {
        note: noteParent,
        isInPlaceEditEnabled: true,
      },
    });

    await wrapper.find('#description-id').trigger('click');
    await wrapper.find("#note-undefined").trigger("blur");

    expect(wrapper.findAll('#description-form-id')).toHaveLength(0);
    expect(wrapper.findAll("#description-id")).toHaveLength(1);
  });

  it("should update Indonesian description on blur when language is Indonesian", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").description("Dummy Description").please();
    store.commit("loadNotes", [noteParent]);

    const { wrapper } = mountWithStoreAndMockRoute(
      store,
      NoteContent,
      { 
        props: { 
          note: noteParent,
          language: Languages.ID,
          isInPlaceEditEnabled: true 
        } 
      },
    );

    await wrapper.find('#description-id').trigger('click');
    await wrapper.find('#description-form-id textarea').setValue('Dummy Description Updated');
    await wrapper.find('#description-form-id textarea').trigger('textarea');
    await wrapper.find('#note-undefined').trigger("blur");

    expect(wrapper.find("#description-id").text()).toContain('Dummy Description Updated');
  });
});

