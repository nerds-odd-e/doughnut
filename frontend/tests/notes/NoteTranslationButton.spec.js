/**
 * @jest-environment jsdom
 */

import NoteOverview from "@/components/notes/NoteOverview.vue";
import NoteTranslationButton from "@/components/toolbars/NoteTranslationButton.vue";
import makeMe from "../fixtures/makeMe";
import { mount } from "@vue/test-utils";
import { renderWithStoreAndMockRoute } from "../helpers";
import store from "../../src/store/index.js";

describe("Note Translation Button", () => {
    let wrapper = null;
    let component = null;
    let buttonTranslation = null;

    beforeAll(() => {
        const note = makeMe.aNote.title("Notes 1").please();
        store.commit("loadNotes", [note]);
        renderWithStoreAndMockRoute(
            store,
            NoteOverview,
            { props: { noteId: note.id, expandChildren: true } },
        );
        
        wrapper = mount(NoteTranslationButton);
        component = wrapper.findComponent({ name: "NoteTranslationButton" });
        buttonTranslation = component.find(".btn-translation");
    });
    
    it("should show translation button", async () => {
        expect(component.exists()).toBe(true);
    });

    it("should show ID translation button as default", async () => {
        expect(buttonTranslation.attributes().lang).toBe("EN");
        expect(buttonTranslation.find(".flag").attributes().class).toContain("ID");
    });

    it("should show EN translation button when ID translation button is clicked", async () => {
        expect(buttonTranslation.find(".flag").attributes().class).toContain("ID");
        
        await wrapper.find(".btn-translation").trigger("click");
        
        expect(wrapper.find(".btn-translation").attributes().lang).toBe("ID");
        expect(wrapper.find(".btn-translation .flag").attributes().class).toContain("EN");
    });

    it("should show ID translation button when EN translation button is clicked", async () => {
        expect(buttonTranslation.find(".flag").attributes().class).toContain("EN");

        await wrapper.find(".btn-translation").trigger("click");
        
        expect(wrapper.find(".btn-translation").attributes().lang).toBe("EN");
        expect(wrapper.find(".btn-translation .flag").attributes().class).toContain("ID");
    });
 });