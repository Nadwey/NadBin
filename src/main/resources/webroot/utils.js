function createLink(href, text) {
    const link = document.createElement("a");
    link.href = href;
    link.innerText = text;
    return link;
}

function createSpan(text) {
    const span = document.createElement("span");
    span.innerText = text;
    return span;
}

function addColumn(row, element) {
    let td = document.createElement("td");
    td.appendChild(element);
    row.appendChild(td);
}
