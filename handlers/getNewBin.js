const fs = require("fs");

/**
 * Handles GET /new-bin
 * 
 * @param {import("fastify").FastifyRequest} req 
 * @param {import("fastify").FastifyReply} reply 
 */
function getNewBin(req, reply) {
    reply.redirect(302, `/${crypto.randomUUID()}`);
}

module.exports = getNewBin;
