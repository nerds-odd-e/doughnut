import LinksBuilder from "./LInksBuilder";
import NoteBuilder from "./NoteBuilder";

class MakeMe {
    get links(): LinksBuilder {
        return new LinksBuilder(null)
    }
    get aNote(): NoteBuilder {
        return new NoteBuilder(null)
    }
}
    
const makeMe = new MakeMe

export default makeMe;
