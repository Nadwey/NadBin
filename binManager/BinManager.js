const { QuickDB } = require("quick.db");
const fs = require("fs");

/**
 * @typedef {Object} File
 * @property {string} name name of the file
 * @property {string} localPath path to the file on the server
 * @property {number} size size of the file in bytes
 */

/**
 * @typedef {Object} Bin
 * @property {string} id id of the bin
 * @property {string} createdAt iso string of when the bin was created
 * @property {boolean} isLocked
 * @property {File[]} files
 */

/**
 * Creates a File object
 *
 * @param {string} name
 * @param {string} localPath
 * @param {number} size
 * @param {string} uploadedAt
 * @returns {File}
 */
function makeFile(name, localPath, size, uploadedAt) {
    return {
        name,
        localPath,
        size,
        uploadedAt,
    };
}

/**
 * Creates a Bin object
 *
 * @param {string} id
 * @param {string} createdAt
 * @param {boolean} isLocked
 * @param {File[]} files
 * @returns {Bin}
 */
function makeBin(id, createdAt, isLocked) {
    return {
        id,
        createdAt,
        isLocked,
        files: [],
    };
}

class BinManager {
    constructor() {}

    async init() {
        if (BinManager.db) return;

        BinManager.db = new QuickDB({ filePath: "files.sqlite" });
        BinManager.db.init();
    }

    async addFile(bin, file) {
        if (!(await BinManager.db.has(bin))) await BinManager.db.set(bin, makeBin(bin, new Date().toISOString(), false));

        const binData = await BinManager.db.get(bin);
        await BinManager.db.set(bin, {
            ...binData,
            files: [...(binData.files || []), file],
        });
    }

    /**
     *
     * @param {string} bin
     * @returns {Bin}
     */
    async getBin(bin) {
        return await BinManager.db.get(bin);
    }

    /**
     * Returns file if it exists
     *
     * @param {string} bin
     * @param {string} fileName
     * @returns {File}
     */
    async getFile(bin, fileName) {
        const binData = await BinManager.db.get(bin);
        if (!binData) return null;
        if (!Array.isArray(binData.files)) return null;

        return binData.files.find((file) => file.name === fileName);
    }

    async deleteFile(bin, fileName) {
        const fileData = await this.getFile(bin, fileName);
        if (!fileData) return false;

        const binData = await BinManager.db.get(bin);
        const fileIndex = binData.files.findIndex((file) => file.name === fileName);
        binData.files.splice(fileIndex, 1);
        await BinManager.db.set(bin, binData);

        fs.rmSync(fileData.localPath, { force: true });

        return true;
    }

    async deleteBin(bin) {
        const binData = await BinManager.db.get(bin);
        if (!binData) return false;

        if (Array.isArray(binData.files)) {
            for await (const file of binData.files) {
                fs.rmSync(file.localPath, { force: true });
            }
        }

        await BinManager.db.delete(bin);
    }

    /**
     * Checks if a bin name is valid
     *
     * @param {string} name
     * @returns {boolean}
     */
    static isValidBinName(name) {
        const BIN_NAME_REGEX = /^[a-zA-Z0-9-_]+$/;

        if (name.length < 16) return false;
        if (!BIN_NAME_REGEX.test(name)) return false;

        return true;
    }

    /**
     * @type {QuickDB}
     */
    static db = null;
}

module.exports = BinManager;
module.exports.makeFile = makeFile;
module.exports.makeBin = makeBin;
