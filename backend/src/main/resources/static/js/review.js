async function getNotes() {
    const response = await fetch('/getNotes');
    const noteJson = await response.json(); 
    document.getElementById("note-title").innerHTML = noteJson[0].title;
    document.getElementById("note-description").innerHTML = noteJson[0].description;
}