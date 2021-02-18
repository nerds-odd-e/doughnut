let noteList = [];
let currentIndex = 0;

async function getNotes() {
    const response = await fetch('/getNotes');
    noteList = await response.json(); 
    nextNote();
}

function nextNote() {
    if (currentIndex >= noteList.length) {
        currentIndex = 0;
    }

    document.getElementById("note-title").innerHTML = noteList[currentIndex].title;
    document.getElementById("note-description").innerHTML = noteList[currentIndex].description;
    currentIndex++;
}