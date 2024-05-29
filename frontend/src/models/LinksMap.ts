import { LinkViewed, Note } from "@/generated/backend";

type LinksMap = { [P in Note.linkType]?: LinkViewed };

export default LinksMap;
