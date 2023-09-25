const axios = require("axios").default;
const child_process = require("child_process");

let pid = null;

beforeAll(async () => {
    return new Promise(async(resolve, reject) => {
        pid = child_process.exec("node index.js").pid;

        setTimeout(resolve, 5000);
    });
}, 10000);

describe("Tests GET /new-bin", () => {
    test("Test redirect to random bin", async () => {
        const req = await axios.get("http://localhost:7000/new-bin", {
            validateStatus: () => true,
        });

        const binPath = req.request.path;
        expect(binPath).not.toBe("/new-bin");
    });
});

afterAll(() => {
    return new Promise((resolve, reject) => {
        process.kill(pid);

        setTimeout(resolve, 5000);
    });
}, 10000);
