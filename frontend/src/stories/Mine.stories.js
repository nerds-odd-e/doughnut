import Svg from '../components/svgs/link_types/SvgLinkTypeSpecialize.vue';
import SvgLinkTypeIcon from '../components/svgs/SvgLinkTypeIcon.vue';
import NoteShow from '../components/notes/NoteShow.vue';
import {colors} from '../colors';
import {linkTypeOptions} from '../../tests/notes/fixtures-basic';

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
    return { types: linkTypeOptions }
  },
  template: `
  <div v-for="type in types" :key="type.value">
  {{type.label}}
  <SvgLinkTypeIcon :linkTypeId="0+type.value" width="80px" height="40px"/>
  <SvgLinkTypeIcon :linkTypeId="0+type.value" width="80px" height="40px" :inverseIcon="true"/>
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

const noteData = {
  "note": {
    "id": 1743,
    "noteContent": {
      "id": 1743,
      "title": "そうだ (伝聞)",
      "description": "接続\r\n\r\n　①伝聞\r\n　名詞＋だそう／だったそう\r\n　ナ形語幹＋なそうだ／だそうだ\r\n　イ形普通形＋そうだ\r\n　動詞普通形＋そうだ\r\n\r\n意味\r\n\r\n　①听说\r\n　　（据说这家店可以自由的打包带回家。）\r\n　（３）　お隣さん、子どもがもうすぐ生まれるそうだ。\r\n　　　　　（听说隔壁的孩子马上就要出生了。）\r\n　（４）　医者によると、 とても難しい手術だったそうだ。\r\n　　　　　（据医生说，手术非常难。）\r\n　（５）　Ａくん昨日事故に遭ったそうだ。だから今日休んでいるみたい。\r\n　　　　　（听说A君昨天遭遇事故了。所以今天好像休息了。）\r\n　（６）　今回の試験の合格率は１５％だったそうだ。\r\n　　　　　（听说这次考试的合格率是15%。）\r\n　（７）　隣町は大雨だったそうだが、こっちは快晴だった。\r\n　　　　　（听说邻镇下了大雨，但是我们这里是晴朗的天气。）\r\n　（８）　そこの洋食屋さん、ネットで人気で美味しいそうだ。\r\n　　　　　（听说那里的西餐厅在网上很受欢迎，好像很好吃。）",
      "url": "",
      "urlIsVideo": false,
      "useParentPicture": false,
      "skipReview": false,
      "hideTitleInArticle": false,
      "showAsBulletInArticle": false,
      "updatedAt": "2021-07-29T06:16:07.000+00:00"
    },
    "createdAt": "2021-06-04T23:21:04.000+00:00",
    "title": "そうだ (伝聞)",
    "notePicture": "https://livedoor.blogimg.jp/edewakaru/imgs/d/0/d0ec0e9a-s.jpg",
    "parentId": 1503,
    "head": false,
    "noteTypeDisplay": "Child Note",
    "shortDescription": "接続\r\n\r\n　①伝聞\r\n　名詞＋だそう／だったそう\r\n　ナ形語幹＋なそうだ／だそうだ\r\n　イ形..."
  },
  "notebook": {
    "id": 15,
    "ownership": {
      "id": 1,
      "circle": null,
      "fromCircle": false
    },
    "skipReviewEntirely": false,
    "notebookType": "GENERAL"
  },
  "links": {
    "related to": {
      "direct": [
        {
          "id": 510,
          "targetNote": {
            "id": 1819,
            "createdAt": "2021-06-07T12:05:02.000+00:00",
            "title": "そうだ・ようだ・らしい・みたい",
            "notePicture": "/images/73/EF9079DB-2E06-4E0A-B4D7-522C44AF6154.jpeg",
            "parentId": 115,
            "head": false,
            "noteTypeDisplay": "Child Note",
            "shortDescription": "看完以上五篇後，我們可以總歸一個重點：\r\n\r\n（傳聞）そうだ：將聽來或看到的情報轉述他人\r\n（..."
          },
          "typeId": 1,
          "createdAt": "2021-06-07T12:06:04.000+00:00",
          "linkTypeLabel": "related to",
          "linkNameOfSource": "related note"
        }
      ],
      "reverse": []
    },
    "tagged by": {
      "direct": [
        {
          "id": 598,
          "targetNote": {
            "id": 1782,
            "createdAt": "2021-06-06T00:37:31.000+00:00",
            "title": "客観 / きゃっかん",
            "notePicture": "",
            "parentId": 1781,
            "head": false,
            "noteTypeDisplay": "Child Note",
            "shortDescription": ""
          },
          "typeId": 8,
          "createdAt": "2021-06-09T23:18:39.000+00:00",
          "linkTypeLabel": "tagged by",
          "linkNameOfSource": "tag target"
        },
        {
          "id": 1435,
          "targetNote": {
            "id": 2217,
            "createdAt": "2021-06-27T22:45:10.000+00:00",
            "title": "伝聞",
            "notePicture": null,
            "parentId": 1818,
            "head": false,
            "noteTypeDisplay": "Child Note",
            "shortDescription": null
          },
          "typeId": 8,
          "createdAt": "2021-06-27T22:45:38.000+00:00",
          "linkTypeLabel": "tagged by",
          "linkNameOfSource": "tag target"
        }
      ],
      "reverse": []
    },
    "confused with": {
      "direct": [],
      "reverse": [
        {
          "id": 436,
          "sourceNote": {
            "id": 1748,
            "createdAt": "2021-06-04T23:23:43.000+00:00",
            "title": "そうだ(様態と可能性)",
            "notePicture": "",
            "parentId": 1503,
            "head": false,
            "noteTypeDisplay": "Child Note",
            "shortDescription": ""
          },
          "typeId": 23,
          "createdAt": "2021-06-04T23:24:32.000+00:00",
          "linkTypeLabel": "confused with",
          "linkNameOfSource": "thing"
        },
        {
          "id": 445,
          "sourceNote": {
            "id": 1752,
            "createdAt": "2021-06-05T23:17:34.000+00:00",
            "title": "らしい (伝聞／推測)",
            "notePicture": "https://livedoor.blogimg.jp/edewakaru/imgs/0/a/0a362cbe-s.jpg",
            "parentId": 1501,
            "head": false,
            "noteTypeDisplay": "Child Note",
            "shortDescription": "意味\r\n\r\n　①好像…\r\n　　似乎…\r\n　\r\n　①伝聞／推測\r\n　他人から聞いた情報や、状況か..."
          },
          "typeId": 23,
          "createdAt": "2021-06-05T01:00:30.000+00:00",
          "linkTypeLabel": "confused with",
          "linkNameOfSource": "thing"
        }
      ]
    },
    "using": {
      "direct": [
        {
          "id": 1938,
          "targetNote": {
            "id": 2423,
            "createdAt": "2021-07-06T13:27:15.000+00:00",
            "title": "普通形[な/である]+〜",
            "notePicture": null,
            "parentId": 2369,
            "head": false,
            "noteTypeDisplay": "Child Note",
            "shortDescription": null
          },
          "typeId": 15,
          "createdAt": "2021-07-08T12:54:16.000+00:00",
          "linkTypeLabel": "using",
          "linkNameOfSource": "user"
        }
      ],
      "reverse": []
    },
    "similar to": {
      "direct": [],
      "reverse": [
        {
          "id": 3556,
          "sourceNote": {
            "id": 2216,
            "createdAt": "2021-06-27T22:44:04.000+00:00",
            "title": "〜って/〜んだって（伝聞）",
            "notePicture": "https://livedoor.blogimg.jp/edewakaru/imgs/6/d/6d9daff5-s.jpg",
            "parentId": 2212,
            "head": false,
            "noteTypeDisplay": "Child Note",
            "shortDescription": "【〜って・〜んだって＝〜そうだ・〜らしい】\r\n\r\n［例文］\r\n①あそこのパンはおいしいんだって..."
          },
          "typeId": 22,
          "createdAt": "2021-08-14T00:18:30.000+00:00",
          "linkTypeLabel": "similar to",
          "linkNameOfSource": "thing"
        }
      ]
    },
    "a specialization of": {
      "direct": [
        {
          "id": 1254,
          "targetNote": {
            "id": 1503,
            "createdAt": "2021-06-04T23:04:46.000+00:00",
            "title": "そうだ",
            "notePicture": "",
            "parentId": 1604,
            "head": false,
            "noteTypeDisplay": "Child Note",
            "shortDescription": "　２つの意味があります。それぞれ接続が違うので注意してください。\n\n　①伝聞\n　②様態\n　"
          },
          "typeId": 2,
          "createdAt": "2021-06-24T13:57:41.000+00:00",
          "linkTypeLabel": "a specialization of",
          "linkNameOfSource": "specification"
        }
      ],
      "reverse": [
        {
          "id": 1737,
          "sourceNote": {
            "id": 2374,
            "createdAt": "2021-07-05T13:19:59.000+00:00",
            "title": "～によると[～によれば]、〜そうだ",
            "notePicture": null,
            "parentId": 1743,
            "head": false,
            "noteTypeDisplay": "Child Note",
            "shortDescription": "前件には「～によると」「～によれば」などの情報の根源を示す内容が呼応しやすいです。"
          },
          "typeId": 2,
          "createdAt": "2021-07-05T13:19:59.000+00:00",
          "linkTypeLabel": "a specialization of",
          "linkNameOfSource": "specification"
        }
      ]
    },
    "an attribute of": {
      "direct": [],
      "reverse": [
        {
          "id": 1023,
          "sourceNote": {
            "id": 2044,
            "createdAt": "2021-06-20T10:38:16.000+00:00",
            "title": "伝聞のらしい、そうだ",
            "notePicture": "",
            "parentId": 2217,
            "head": false,
            "noteTypeDisplay": "Child Note",
            "shortDescription": "らしいは【見たり聞いたりして推量】【伝聞情報で推量】\r\n\r\n 「そうだ」（傳聞）是將聽到或看到..."
          },
          "typeId": 10,
          "createdAt": "2021-06-20T10:39:04.000+00:00",
          "linkTypeLabel": "an attribute of",
          "linkNameOfSource": "attribute"
        }
      ]
    },
    "an example of": {
      "direct": [],
      "reverse": [
        {
          "id": 1736,
          "sourceNote": {
            "id": 2373,
            "createdAt": "2021-07-05T13:16:24.000+00:00",
            "title": "天気予報によると、所によっては大雨になるそうだ。",
            "notePicture": null,
            "parentId": 393,
            "head": false,
            "noteTypeDisplay": "Child Note",
            "shortDescription": "（根据天气预报，有的地方会下大雨。）"
          },
          "typeId": 17,
          "createdAt": "2021-07-05T13:17:39.000+00:00",
          "linkTypeLabel": "an example of",
          "linkNameOfSource": "example"
        }
      ]
    }
  },
  "navigation": {
    "previousSiblingId": null,
    "previousId": 1503,
    "nextId": 2174,
    "nextSiblingId": 1744
  },
  "ancestors": [
    {
      "id": 392,
      "createdAt": "2021-03-26T23:29:10.000+00:00",
      "title": "日本語",
      "notePicture": "",
      "parentId": null,
      "head": true,
      "noteTypeDisplay": "Child Note",
      "shortDescription": ""
    },
    {
      "id": 1413,
      "createdAt": "2021-05-13T00:03:29.000+00:00",
      "title": "単語",
      "notePicture": "",
      "parentId": 392,
      "head": false,
      "noteTypeDisplay": "Child Note",
      "shortDescription": ""
    },
    {
      "id": 1594,
      "createdAt": "2021-05-26T22:56:26.000+00:00",
      "title": "品詞",
      "notePicture": "https://stat.ameba.jp/user_images/20200217/21/i-wataame/be/18/j/o0776136114714766926.jpg?caw=800",
      "parentId": 1413,
      "head": false,
      "noteTypeDisplay": "Child Note",
      "shortDescription": ""
    },
    {
      "id": 1604,
      "createdAt": "2021-05-29T22:27:49.000+00:00",
      "title": "助動詞",
      "notePicture": "",
      "parentId": 1594,
      "head": false,
      "noteTypeDisplay": "Child Note",
      "shortDescription": "活用ありの付属語"
    },
    {
      "id": 1503,
      "createdAt": "2021-06-04T23:04:46.000+00:00",
      "title": "そうだ",
      "notePicture": "",
      "parentId": 1604,
      "head": false,
      "noteTypeDisplay": "Child Note",
      "shortDescription": "　２つの意味があります。それぞれ接続が違うので注意してください。\n\n　①伝聞\n　②様態\n　"
    }
  ],
  "children": [
    {
      "id": 2174,
      "createdAt": "2021-06-26T00:09:02.000+00:00",
      "title": "そうだ伝聞の否定",
      "notePicture": "",
      "parentId": 1743,
      "head": false,
      "noteTypeDisplay": "Child Note",
      "shortDescription": "［伝聞］を表す「〜そうだ」の否定形は「〜ないそうだ」「〜なかったそうだ」になりますので、気をつ..."
    },
    {
      "id": 2374,
      "createdAt": "2021-07-05T13:19:59.000+00:00",
      "title": "～によると[～によれば]、〜そうだ",
      "notePicture": null,
      "parentId": 1743,
      "head": false,
      "noteTypeDisplay": "Child Note",
      "shortDescription": "前件には「～によると」「～によれば」などの情報の根源を示す内容が呼応しやすいです。"
    }
  ],
  "owns": true
}

const TemplateNoteShow = args => ({
  components: { NoteShow
   },
  data() {
    return {noteData: noteData, colors, linkTypeOptions};
  },
  setup() {
    return { args, ...actionsData };
  },
  template: `
  <NoteShow v-bind="noteData" :staticInfo="{linkTypeOptions, colors}"/>
  `,
});
export const NoteShowStory = TemplateNoteShow.bind({});