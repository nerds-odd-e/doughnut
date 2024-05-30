import { LinkViewed, NoteTopic } from "@/generated/backend";

type LinksMap = { [P in NoteTopic.linkType]?: LinkViewed };

export default LinksMap;
