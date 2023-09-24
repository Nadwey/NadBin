const fastify = require("fastify")();
const fs = require("node:fs");
const path = require("path");

fastify.register(require("@fastify/multipart"), {
    limits: {
        fileSize: 1024 * 1024 * 1024 * 10, // 10 GiB
    },
});

fastify.register(require("@fastify/static"), {
    root: path.join(process.cwd(), "static"),
    prefix: "/",
    wildcard: false,
});

// handlers
const getBin = require("./handlers/getBin");
const getBinFile = require("./handlers/getBinFile");
const postBinFile = require("./handlers/postBinFile");
const deleteBinFile = require("./handlers/deleteBinFile");
const getNewBin = require("./handlers/getNewBin");
const BinManager = require("./binManager/BinManager");

fastify.get("/:bin", getBin);
fastify.get("/:bin/:file", getBinFile);
fastify.post("/:bin/:file", postBinFile);
fastify.delete("/:bin/:file", deleteBinFile);
fastify.get("/new-bin", getNewBin);

fastify.setErrorHandler(async (error, request, reply) => {
    console.log(error);
});

(async () => {
    const binManager = new BinManager();
    await binManager.init();

    fastify.listen({ port: 7000 }, (err) => {
        if (err) throw err;
        console.log(`server listening on ${fastify.server.address().port}`);
    });
})();
