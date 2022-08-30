/**
 * @jest-environment jsdom
 */
import NoteMinmap from "@/components/notes/mindmap/NoteMindmap.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";
import NoteRealmCache from "../../src/store/NoteRealmCache";

describe("note mindmap", () => {
  const notes: Generated.NoteRealm[] = [];

  beforeEach(() => {
    notes.length = 0;
    helper.reset();
  });

  const getMountedElement = (noteId: Doughnut.ID, props = {}) => {
    return helper
      .component(NoteMinmap)
      .withStorageProps({
        noteId,
        noteRealms: new NoteRealmCache({
          notes,
          notePosition: makeMe.aNotePosition.please(),
        }),
        offset: { scale: 1, rotate: 0 },
        ...props,
      })
      .mount();
  };

  it("should render one note", async () => {
    notes.push(
      makeMe.aNoteRealm
        .title("single note")
        .shortDescription("not long")
        .please()
    );
    const wrapper = getMountedElement(notes[0].id);
    expect(wrapper.find("[role='card']").text()).toContain("single note");
  });

  describe("with two notes", () => {
    beforeEach(() => {
      const note = makeMe.aNoteRealm.title("note1").please();
      const childNote = makeMe.aNoteRealm.title("note2").under(note).please();
      notes.push(note);
      notes.push(childNote);
    });

    it("should render the two notes", async () => {
      const wrapper = getMountedElement(notes[0].id);
      expect(wrapper.findAll("[role='card']")).toHaveLength(2);
    });

    it("should connect the two notes", async () => {
      const wrapper = getMountedElement(notes[0].id);
      const connection = wrapper.find("svg.mindmap-canvas");
      const line = connection.find("line");
      expect(parseFloat(line.attributes("x2") as string)).toBeCloseTo(0);
      expect(parseFloat(line.attributes("y2") as string)).toBeCloseTo(185);
    });

    describe("with two grandchildren notes", () => {
      beforeEach(() => {
        const childNote = notes[1];
        notes.push(makeMe.aNoteRealm.title("grand1").under(childNote).please());
        notes.push(makeMe.aNoteRealm.title("grand2").under(childNote).please());
      });

      it("should connect the two notes", async () => {
        const wrapper = getMountedElement(notes[0].id);
        const connection = await wrapper.find("svg.mindmap-canvas");
        const lines = connection.findAll("line");
        expect(lines).toHaveLength(3);
        const lastLine = lines[2];
        expect(parseFloat(lastLine.attributes("x1") as string)).toBeCloseTo(
          -75
        );
        expect(parseFloat(lastLine.attributes("y1") as string)).toBeCloseTo(
          198.1212
        );
        expect(parseFloat(lastLine.attributes("y2") as string)).toBeCloseTo(
          189.0275953
        );
      });
    });
    describe("links between notes", () => {
      beforeEach(() => {
        const [top, child1] = notes;
        const child2 = makeMe.aNoteRealm
          .title("child2")
          .under(top)
          .linkTo(child1)
          .please();
        notes.push(child2);
      });

      it("should link the two linked notes", async () => {
        const wrapper = getMountedElement(notes[0].id);
        const connection = wrapper.find("svg.mindmap-canvas");
        const linkStart = connection.findAll(".link-start");
        expect(linkStart).toHaveLength(2);
        expect(linkStart[0].attributes("transform")).toEqual(
          "translate(285, 0) rotate(0)"
        );
        expect(linkStart[1].attributes("transform")).toEqual(
          "translate(-135, 0) rotate(360)"
        );
      });
    });

    describe("links between note and note outside the map", () => {
      it("link target is not on the map", async () => {
        const [top] = notes;
        const noteThatIsNotOnTheMap = makeMe.aNoteRealm
          .title("not on the map")
          .please();
        const child2 = makeMe.aNoteRealm
          .title("child2")
          .under(top)
          .linkTo(noteThatIsNotOnTheMap)
          .please();
        notes.push(noteThatIsNotOnTheMap);
        notes.push(child2);
        const wrapper = getMountedElement(notes[0].id);
        const connection = wrapper.find("svg.mindmap-canvas");
        const lines = connection.findAll("g.notes-link line");
        expect(lines).toHaveLength(0);
      });
    });
  });

  describe("size", () => {
    beforeEach(() => {
      notes.push(
        makeMe.aNoteRealm
          .title("single note")
          .picture("a.jpg")
          .shortDescription("not long")
          .please()
      );
    });

    it("small size by default", async () => {
      const wrapper = getMountedElement(notes[0].id, {
        offset: { scale: 1, rotate: 0 },
      });
      const descriptionIndicators = wrapper.findAll(".description-indicator");
      expect(descriptionIndicators).toHaveLength(1);
      const description = wrapper.findAll(".note-description");
      expect(description).toHaveLength(0);
      const pictureIndicators = wrapper.findAll(".picture-indicator");
      expect(pictureIndicators).toHaveLength(1);
      const pictures = wrapper.findAll(".note-picture");
      expect(pictures).toHaveLength(0);
    });

    it("medium", async () => {
      const wrapper = getMountedElement(notes[0].id, {
        offset: { scale: 1.5, rotat: 0 },
      });
      const descriptionIndicators = wrapper.findAll(".description-indicator");
      expect(descriptionIndicators).toHaveLength(0);
      const description = wrapper.findAll(".note-description");
      expect(description).toHaveLength(0);
      const shortDescription = wrapper.findAll(".note-short-description");
      expect(shortDescription).toHaveLength(1);
    });

    it("large", async () => {
      const wrapper = getMountedElement(notes[0].id, {
        offset: { scale: 2.1, rotate: 0 },
      });
      const descriptionIndicators = wrapper.findAll(".description-indicator");
      expect(descriptionIndicators).toHaveLength(0);
      const description = wrapper.findAll(".note-description");
      expect(description).toHaveLength(1);
    });
  });
});
