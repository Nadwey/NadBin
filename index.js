const fastify = require("fastify")();
const fs = require("node:fs");
const path = require("path");
require("dotenv").config();

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
const deleteBin = require("./handlers/deleteBin");
const getBinFile = require("./handlers/getBinFile");
const postBinFile = require("./handlers/postBinFile");
const deleteBinFile = require("./handlers/deleteBinFile");
const getNewBin = require("./handlers/getNewBin");
const BinManager = require("./binManager/BinManager");

fastify.get("/:bin", getBin);
fastify.delete("/:bin", deleteBin);
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

    if (typeof PhusionPassenger !== "undefined" || process.env.PHUSIONPASSENGER) {
        fastify.listen({ path: "passenger", host: "127.0.0.1" }, (err) => {
            if (err) throw err;
            console.log(`PhusionPassenger detected, listening on `, fastify.server.address().toString());
        });
    } else {
        fastify.listen({ port: process.env.PORT || 7000 }, (err) => {
            if (err) throw err;
            console.log("NadBin listening on port", fastify.server.address().port);
        });
    }
})();
