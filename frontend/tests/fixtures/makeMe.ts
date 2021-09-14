import LinksBuilder from "./LInksBuilder";

class MakeMe {
    get links(): LinksBuilder {
        return new LinksBuilder(null)
    }
}
    
const makeMe = new MakeMe

export default makeMe;
