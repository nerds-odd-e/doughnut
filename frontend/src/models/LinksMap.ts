import { LinkViewed, Thing } from "@/generated/backend";

type LinksMap = { [P in Thing.linkType]?: LinkViewed };

export default LinksMap;
