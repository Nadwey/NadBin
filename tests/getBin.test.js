const axios = require("axios").default;
const child_process = require("child_process");

let serverProcess = null;

beforeAll(async () => {
    return new Promise(async(resolve, reject) => {
        serverProcess = child_process.exec("node index.js");

        setTimeout(resolve, 5000);
    });
}, 10000);

describe("Tests GET /:bin", () => {
    test("Not existing bin", async () => {
        const req = await axios.get("http://localhost:7000/thisbinshouldnotexist", {
            validateStatus: () => true,
        });

        expect(req.status).toBe(404);
    });
});

afterAll(() => {
    return new Promise((resolve, reject) => {
        serverProcess.kill();

        setTimeout(resolve, 5000);
    });
}, 10000);
