import Svg from '../components/svgs/link_types/SvgLinkTypeSpecialize.vue';
import SvgLinkTypeSpecialize from '../components/svgs/link_types/SvgLinkTypeSpecialize.vue';
import SvgLinkTypeRelated from '../components/svgs/link_types/SvgLinkTypeRelated.vue';
import SvgLinkTypeApplication from '../components/svgs/link_types/SvgLinkTypeApplication.vue';

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
  components: { Svg, SvgLinkTypeSpecialize, SvgLinkTypeRelated, SvgLinkTypeApplication },
  setup() {
    return { args, ...actionsData };
  },
  template: `
  <SvgLinkTypeSpecialize v-bind="args" />
  <SvgLinkTypeRelated v-bind="args" />
  <SvgLinkTypeApplication v-bind="args" />
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