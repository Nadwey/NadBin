const fs = require("fs");
const BinManager = require("../binManager/BinManager");

/**
 * Returns Bin object without any data that shouldn't be public 
 * 
 * @param {import("../binManager/BinManager").Bin} bin 
 * @returns 
 */
function publicBinData(bin) {
    return {
        id: bin.id,
        createdAt: bin.createdAt,
        isLocked: bin.isLocked,
        files: bin.files.map((file) => ({
            name: file.name,
            size: file.size,
            uploadedAt: file.uploadedAt,
        })),
    };
}

/**
 * Handles GET /:bin
 *
 * @param {import("fastify").FastifyRequest} req
 * @param {import("fastify").FastifyReply} reply
 */
async function getBin(req, reply) {
    if (!BinManager.isValidBinName(req.params.bin))
        return reply.status(400).send("Invalid bin name\nBin names must be at least 16 characters long and can only contain alphanumeric characters, dashes, and underscores");

    const binManager = new BinManager();
    const bin = await binManager.getBin(req.params.bin);

    if (req?.headers["accept"]?.toLocaleLowerCase?.() === "application/json") {
        if (bin === null) return reply.status(404).send("Bin not found");

        return reply.type("application/json").send(publicBinData(bin));
    }

    return reply.type("text/html").status(bin === null ? 404 : 200).send(fs.createReadStream("pages/bin.html"));
}

module.exports = getBin;
