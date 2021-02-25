let noteList = [];
let currentIndex = 0;

function transformNoteListToView(noteList) {
    const result = [];
    noteList.forEach((noteItem) => {
        const { title, description, picture } = noteItem;

        if (noteItem.targetNotes?.length) {
            result.push({
                title,
                links: noteItem.targetNotes.map((targetNote) => targetNote.title),
            })
        }

        result.push({ title, description, picture });
    });

    return result;
}

async function populateAllNotesForReview() {
    const containers= document.getElementsByClassName("review-container");
    if (containers.length === 0) {
        return;
    }
    const response = await fetch('/getNotes');
    noteList = transformNoteListToView(await response.json());
    nextNote();
}

function nextNote() {
    if (currentIndex >= noteList.length) {
        currentIndex = 0;
    }

    const { title, description, picture, links } = noteList[currentIndex];

    document.getElementById("note-title").innerHTML = title;

    const descElem = document.getElementById("note-description");
    const pictureElem = document.getElementById("note-picture");
    const listElem = document.getElementById("note-links");
    const listLabelElem = document.getElementById("note-links-label");

    if (description) {
        descElem.innerHTML = description;
    } else {
        descElem.innerHTML = "";
    }

    if (picture) {
        pictureElem.innerHTML = "<img src='"+ picture + "'/>";
    } else {
        pictureElem.innerHTML = "";
    }

    if (links) {
        listElem.innerHTML = links.reduce((currentString, title) => `${currentString}<li class="list-group-item">${title}</li>`, "")
        listLabelElem.innerHTML = "Links:";
    } else {
        listElem.innerHTML = "";
        listLabelElem.innerHTML = "";
    }
    currentIndex++;
}