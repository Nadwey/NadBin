const axios = require("axios").default;
const child_process = require("child_process");

let serverProcess = null;

beforeAll(async () => {
    return new Promise(async(resolve, reject) => {
        serverProcess = child_process.spawn("node index.js");

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
        serverProcess.kill();

        setTimeout(resolve, 5000);
    });
}, 10000);
