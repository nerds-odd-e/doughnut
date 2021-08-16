import Svg from '../components/svgs/link_types/SvgLinkTypeSpecialize.vue';
import SvgLinkTypeIcon from '../components/svgs/SvgLinkTypeIcon.vue';

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
  components: { Svg, SvgLinkTypeIcon
   },
  data() {
    return {
      types: [
        {
            "reversedLabel": "related to",
            "label": "related to",
            "value": "1"
        },
        {
            "reversedLabel": "a generalization of",
            "label": "a specialization of",
            "value": "2"
        },
        {
            "reversedLabel": "applied to",
            "label": "an application of",
            "value": "3"
        },
        {
            "reversedLabel": "has instances",
            "label": "an instance of",
            "value": "4"
        },
        {
            "reversedLabel": "has parts",
            "label": "a part of",
            "value": "6"
        },
        {
            "reversedLabel": "tagging",
            "label": "tagged by",
            "value": "8"
        },
        {
            "reversedLabel": "has attributes",
            "label": "an attribute of",
            "value": "10"
        },
        {
            "reversedLabel": "the opposite of",
            "label": "the opposite of",
            "value": "12"
        },
        {
            "reversedLabel": "brought by",
            "label": "author of",
            "value": "14"
        },
        {
            "reversedLabel": "used by",
            "label": "using",
            "value": "15"
        },
        {
            "reversedLabel": "has examples",
            "label": "an example of",
            "value": "17"
        },
        {
            "reversedLabel": "after",
            "label": "before",
            "value": "19"
        },
        {
            "reversedLabel": "similar to",
            "label": "similar to",
            "value": "22"
        },
        {
            "reversedLabel": "confused with",
            "label": "confused with",
            "value": "23"
        }
      ]
    }
  },
  setup() {
    return { args, ...actionsData };
  },
  template: `
  <div v-for="type in types" :key="type.value">
  {{type.label}}
  <SvgLinkTypeIcon :linkTypeId="0+type.value" width="80px" height="40px"/>
  </div>
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