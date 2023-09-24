const BinManager = require("../binManager/BinManager");
const fs = require("fs");

function getLocalPath() {
    let fileLocalPath;

    if (!fs.existsSync("files")) fs.mkdirSync("files");

    do {
        fileLocalPath = `files/${crypto.randomUUID()}`;
    } while (fs.existsSync(fileLocalPath));

    return fileLocalPath;
}

/**
 * Handles POST /:bin/:file
 *
 * @param {import("fastify").FastifyRequest} req
 * @param {import("fastify").FastifyReply} reply
 */
function postBinFile(req, reply) {
    return new Promise(async (resolve, reject) => {
        const { bin, file } = req.params;
        const binManager = new BinManager();

        if (!BinManager.isValidBinName(bin))
            return reply.status(400).send("Invalid bin name\nBin names must be at least 16 characters long and can only contain alphanumeric characters, dashes, and underscores");

        const binData = await binManager.getBin(bin);
        if (binData?.isLocked) return reply.status(403).send("Bin is locked");

        if (await binManager.getFile(bin, file)) return reply.status(409).send("File already exists");

        const data = await req.file();
        if (!data) return reply.status(400).send("No file provided");

        const fileLocalPath = getLocalPath();

        function fail(err) {
            console.error(err);
            fs.rmSync(fileLocalPath, { force: true });
            reply.status(500).send("Failed to upload file");
            reject();
        }

        req.socket.on("error", fail);
        data.file.on("error", fail);

        data.file.on("end", async () => {
            await binManager.addFile(bin, BinManager.makeFile(file, fileLocalPath, data.file.bytesRead, new Date().toISOString()));

            resolve(reply.status(200).send("File uploaded"));
        });

        data.file.pipe(fs.createWriteStream(fileLocalPath));
    });
}

module.exports = postBinFile;
