const BIN_ID = location.pathname.substring(1);
document.getElementById("bin-id").innerText = `/${BIN_ID}`;

/**
 * Fetches the bin files from the server
 *
 * @returns {Promise<Response>}
 */
async function getBinFiles() {
    // the ?bin search param does nothing, it's just here to prevent browsers from displaying json instead of the site sometimes
    return await fetch(`/${BIN_ID}?bin`, {
        method: "GET",
        headers: { Accept: "application/json" },
    });
}

/**
 * Functions that shows or hides the "does not exist" warning
 *
 * @param {boolean} exists
 */
function setBinExists(exists) {
    document.getElementById("doesnt-exist-warning").style.display = exists ? "none" : "block";
    document.getElementById("files").innerHTML = "";
}

/**
 * Creates a file row
 *
 * @param {File} file
 * @returns {HTMLTableRowElement}
 */
function createFileRow(file) {
    const fileName = file.name;
    let tr = document.createElement("tr");

    addColumn(tr, createLink(`/${BIN_ID}/${fileName}`, fileName));
    addColumn(tr, createSpan(file.size));
    addColumn(tr, createSpan(new Date(file.uploadedAt).toLocaleString()));

    let delButton = document.createElement("button");
    delButton.classList.add("delete-button");
    delButton.addEventListener("click", () => {
        fetch(`/${BIN_ID}/${fileName}`, { method: "DELETE" }).then(() => {
            update();
        });
    });
    addColumn(tr, delButton);

    return tr;
}

/**
 * Updates files
 */
async function update() {
    document.getElementById("creation-date").innerText = "";

    const result = await getBinFiles();

    if (result.status === 200) {
        setBinExists(true);

        const data = await result.json();
        document.getElementById("creation-date").innerText = ` - Created ${new Date(data.createdAt).toLocaleString()}`;

        for (const file of data.files) document.getElementById("files").appendChild(createFileRow(file));
    } else if (result.status === 404) {
        setBinExists(false);
    }
}

/**
 * Uploads a file
 *
 * @param {*} file
 */
function uploadFile(file) {
    const uploadProgress = document.createElement("div");
    uploadProgress.classList.add("upload-progress");

    const uploadText = document.createElement("span");

    const uploadBar = document.createElement("div");
    uploadBar.classList.add("upload-bar");

    uploadProgress.appendChild(uploadText);
    uploadProgress.appendChild(uploadBar);

    let xhr = new XMLHttpRequest();
    xhr.upload.addEventListener("progress", function (e) {
        if (e.lengthComputable) {
            const percentComplete = (e.loaded / e.total) * 100;
            uploadText.innerText = `${file.name} - ${percentComplete.toFixed(2)}% | ${(e.loaded / 1024 / 1024).toFixed(3)}MiB / ${(e.total / 1024 / 1024).toFixed(3)}MiB`;
            uploadBar.style.width = `${percentComplete}%`;
        }
    });

    xhr.addEventListener("load", function () {
        update();
        uploadProgress.remove();

        if (xhr.status !== 200) return alert(`Upload failed\n${xhr.responseText}`);
    });

    xhr.addEventListener("error", function () {
        alert("Upload failed");
        uploadProgress.remove();
    });

    xhr.open("POST", `/${BIN_ID}/${file.name}`, true);

    const formData = new FormData();
    formData.append("file", file);

    xhr.send(formData);

    document.getElementById("upload-progresses").appendChild(uploadProgress);
}

async function upload() {
    let fileInput = document.createElement("input");
    fileInput.multiple = true;
    fileInput.type = "file";

    fileInput.onchange = () => {
        for (const file of fileInput.files) uploadFile(file);
    };

    fileInput.click();
}

function remove() {
    fetch(`/${BIN_ID}`, { method: "DELETE" }).then(() => {
        update();
    });
}

update();
