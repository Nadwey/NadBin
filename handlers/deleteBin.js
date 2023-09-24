const BinManager = require("../binManager/BinManager");

/**
 * Handles DELETE /:bin
 *
 * @param {import("fastify").FastifyRequest} req
 * @param {import("fastify").FastifyReply} reply
 */
async function deleteBin(req, reply) {
    const bin = req.params.bin;

    if (!BinManager.isValidBinName(bin))
        return reply.status(400).send("Invalid bin name\nBin names must be at least 16 characters long and can only contain alphanumeric characters, dashes, and underscores");

    const binManager = new BinManager();
    const binData = await binManager.getBin(bin);

    if (binData === null) return reply.status(404).send("Bin not found");

    await binManager.deleteBin(bin);

    return reply.status(200).send("Bin deleted");
}

module.exports = deleteBin;
