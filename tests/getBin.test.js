const axios = require("axios").default;
const child_process = require("child_process");

let pid = null;

beforeAll(async () => {
    return new Promise(async(resolve, reject) => {
        pid = child_process.exec("node index.js").pid;

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
    process.kill(pid);
});
