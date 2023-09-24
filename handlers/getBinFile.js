const fs = require("fs");
const BinManager = require("../binManager/BinManager");
const mime = require("mime-types");

/**
 * Handles GET /:bin/:file
 *
 * @param {import("fastify").FastifyRequest} req
 * @param {import("fastify").FastifyReply} reply
 */
async function getBinFile(req, reply) {
    const { bin, file } = req.params;
    const binManager = new BinManager();

    if (!BinManager.isValidBinName(req.params.bin))
        return reply.status(400).send("Invalid bin name\nBin names must be at least 16 characters long and can only contain alphanumeric characters, dashes, and underscores");

    const binData = await binManager.getBin(bin);
    if (binData === null) return reply.status(404).send("Bin not found");

    const fileData = await binManager.getFile(bin, file);
    if (!fileData) return reply.status(404).send("File not found");

    const stream = fs.createReadStream(fileData.localPath);
    return reply.type(mime.lookup(fileData.name) || "application/octet-stream").send(stream);
}

module.exports = getBinFile;
