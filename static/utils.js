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

// https://gist.github.com/jcouyang/632709f30e12a7879a73e9e132c0d56b
function promiseAllStepN(n, list) {
    let tail = list.splice(n);
    let head = list;
    let resolved = [];
    let processed = 0;
    return new Promise((resolve) => {
        head.forEach((x) => {
            let res = x();
            resolved.push(res);
            res.then((y) => {
                runNext();
                return y;
            });
        });
        function runNext() {
            if (processed == tail.length) {
                resolve(Promise.all(resolved));
            } else {
                resolved.push(
                    tail[processed]().then((x) => {
                        runNext();
                        return x;
                    }),
                );
                processed++;
            }
        }
    });
}