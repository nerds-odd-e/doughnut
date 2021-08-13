import Svg from '../components/svgs/link_types/SvgLinkTypeSpecialize.vue';
import SvgLinkTypeSpecialize from '../components/svgs/link_types/SvgLinkTypeSpecialize.vue';
import SvgLinkTypeRelated from '../components/svgs/link_types/SvgLinkTypeRelated.vue';
import SvgLinkTypeApplication from '../components/svgs/link_types/SvgLinkTypeApplication.vue';
import SvgLinkTypeInstance from '../components/svgs/link_types/SvgLinkTypeInstance.vue';
import SvgLinkTypePart from '../components/svgs/link_types/SvgLinkTypePart.vue';
import SvgLinkTypeSimilar from '../components/svgs/link_types/SvgLinkTypeSimilar.vue';
import SvgLinkTypeExample from '../components/svgs/link_types/SvgLinkTypeExample.vue';
import SvgLinkTypePrecede from '../components/svgs/link_types/SvgLinkTypeInstance.vue';
import SvgLinkTypeUse from '../components/svgs/link_types/SvgLinkTypeInstance.vue';
import SvgLinkTypeAuthor from '../components/svgs/link_types/SvgLinkTypeInstance.vue';
import SvgLinkTypeOpposite from '../components/svgs/link_types/SvgLinkTypeInstance.vue';
import SvgLinkTypeAttr from '../components/svgs/link_types/SvgLinkTypeInstance.vue';
import SvgLinkTypeTagged from '../components/svgs/link_types/SvgLinkTypeTag.vue';

import { action } from '@storybook/addon-actions';

export default {
  component: Svg,
  //👇 Our exports that end in "Data" are not stories.
  excludeStories: /.*Data$/,
  title: 'Svg',
  //👇 Our events will be mapped in Storybook UI
  argTypes: {
    onPinTask: {},
    onArchiveTask: {},
  },
};

export const actionsData = {
  onPinTask: action('pin-task'),
  onArchiveTask: action('archive-task'),
};

const Template = args => ({
  components: { Svg, SvgLinkTypeSpecialize, SvgLinkTypeRelated, SvgLinkTypeApplication, SvgLinkTypeInstance,
    SvgLinkTypePart, SvgLinkTypeSimilar, SvgLinkTypePrecede, SvgLinkTypeExample, SvgLinkTypeUse, SvgLinkTypeAuthor, SvgLinkTypeOpposite, SvgLinkTypeAttr, SvgLinkTypeTagged
   },
  setup() {
    return { args, ...actionsData };
  },
  template: `
  <SvgLinkTypeSpecialize v-bind="args" />
  <SvgLinkTypeRelated v-bind="args" />
  <SvgLinkTypeApplication v-bind="args" />
  <SvgLinkTypeInstance v-bind="args" />
  <SvgLinkTypePart v-bind="args" />
  <SvgLinkTypeSimilar v-bind="args" />
  <SvgLinkTypePrecede v-bind="args" />
  <SvgLinkTypeExample v-bind="args" />
  <SvgLinkTypeUse v-bind="args" />
  <SvgLinkTypeAuthor v-bind="args" />
  <SvgLinkTypeOpposite v-bind="args" />
  <SvgLinkTypeAttr v-bind="args" />
  <SvgLinkTypeTagged v-bind="args" />
  `,
});
export const Default = Template.bind({});
Default.args = {
  task: {
    id: '1',
    title: 'Test Svg',
    state: 'TASK_INBOX',
    updatedAt: new Date(2018, 0, 1, 9, 0),
  },
};

export const Pinned = Template.bind({});
Pinned.args = {
  task: {
    ...Default.args.task,
    state: 'TASK_PINNED',
  },
};

export const Archived = Template.bind({});
Archived.args = {
  task: {
    ...Default.args.task,
    state: 'TASK_ARCHIVED',
  },
};