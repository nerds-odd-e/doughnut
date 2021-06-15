import NoteShow from '@/components/notes/NoteShow.vue';
import { mount } from '@vue/test-utils';
import flushPromises from 'flush-promises';

beforeEach(() => {
  fetch.resetMocks();
});

const stubResponse = {
    "note": {
        "id": 2,
        "noteContent": {
            "id": 2,
            "title": "asdf",
            "description": "asdf",
            "url": "",
            "urlIsVideo": false,
            "pictureUrl": "",
            "pictureMask": "",
            "useParentPicture": false,
            "skipReview": false,
            "hideTitleInArticle": false,
            "showAsBulletInArticle": false,
            "updatedAt": "2021-06-14T11:00:56.000+00:00"
        },
        "createdAt": "2021-06-14T11:00:56.000+00:00",
        "notePicture": "",
        "head": false,
        "title": "asdf"
    },
    "links": {
        "HAS": {
            "direct": [],
            "reverse": [
                {
                    "id": 1,
                    "sourceNote": {
                        "id": 3,
                        "createdAt": "2021-06-14T11:01:06.000+00:00",
                        "notePicture": "",
                        "head": false,
                        "title": "bbb"
                    },
                    "type": "belongs to",
                    "createdAt": "2021-06-14T11:01:26.000+00:00",
                    "linkType": "BELONGS_TO"
                }
            ]
        }
    }
}

describe('note show', () => {

  test('fetch API to be called ONCE', async () => {
    fetch.mockResponseOnce(JSON.stringify(stubResponse));
    const wrapper = mount(NoteShow, {propsData: {noteid: 123}});
    await flushPromises()
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith('/api/notes/123');
    expect(wrapper.findAll(".statistics-value")).toHaveLength(0)
  });
});
