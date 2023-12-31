const BinManager = require("../binManager/BinManager");

/**
 * Handles GET /empty-bin
 *
 * @param {import("fastify").FastifyRequest} req
 * @param {import("fastify").FastifyReply} reply
 */
async function getEmptyBin(req, reply) {
    const binManager = new BinManager();

    let id = null;
    for (;;) {
        id = crypto.randomUUID();
        if (!(await binManager.getBin(id))) break;
    }
    return reply.redirect(302, `/${id}`);
}

module.exports = getEmptyBin;
