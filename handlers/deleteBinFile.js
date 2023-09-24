const BinManager = require("../binManager/BinManager");

/**
 * Handles DELETE /:bin/:file
 *
 * @param {import("fastify").FastifyRequest} req
 * @param {import("fastify").FastifyReply} reply
 */
async function deleteBinFile(req, reply) {
    const { bin, file } = req.params;

    if (!BinManager.isValidBinName(bin))
        return reply.status(400).send("Invalid bin name\nBin names must be at least 16 characters long and can only contain alphanumeric characters, dashes, and underscores");

    const binManager = new BinManager();
    const fileData = await binManager.getFile(bin, file);

    if (fileData === null) return reply.status(404).send("File not found");

    await binManager.deleteFile(bin, file);

    return reply.status(200).send("File deleted");
}

module.exports = deleteBinFile;
