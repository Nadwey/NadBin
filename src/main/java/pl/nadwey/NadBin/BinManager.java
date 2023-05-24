package pl.nadwey.NadBin;

import io.javalin.http.UploadedFile;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.UUID;

public class BinManager {
    BTreeMap fileDB;
    DB dbMaker;

    public BinManager(String dbPath) {
        dbMaker = DBMaker.fileDB(dbPath).make();
        fileDB = dbMaker.treeMap("btree", Serializer.STRING, Serializer.JAVA).createOrOpen();
    }

    public void close() {
        if (!fileDB.isClosed()) fileDB.close();
        if (!dbMaker.isClosed()) dbMaker.close();
    }

    public String uploadFile(UploadedFile uploadedFile) throws IOException {
        File targetFile;

        // generate file until new id
        do {
            targetFile = Paths.get("files", UUID.randomUUID().toString()).toFile();
        } while(targetFile.exists());

        targetFile.getParentFile().mkdirs();

        // get the file from user
        InputStream uploadedFileContent = uploadedFile.content();
        OutputStream outStream = new FileOutputStream(targetFile);
        byte[] buffer = new byte[512 * 1024];
        int bytesRead;
        while ((bytesRead = uploadedFileContent.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }

        outStream.close();
        uploadedFileContent.close();

        return targetFile.toString();
    }

    public void addFileToBin(String binID, String name, String localPath, long size) {
        Bin bin = getBin(binID);
        if (bin == null) {
            bin = new Bin();
            bin.creationDate = LocalDate.now();
        }

        if (binExists(bin, name)) return;

        bin.files.add(new DBFile(name, localPath, size));
        fileDB.put(binID, bin);
    }

    public boolean binExists(String binID) {
        return fileDB.containsKey(binID) && (fileDB.get(binID) != null);
    }

    public boolean binExists(Bin bin, String name) {
        for (final DBFile dbFile : bin.files) {
            if (dbFile.name.equals(name)) return true;
        }

        return false;
    }

    public boolean binExists(String binID, String name) {
        Bin bin = getBin(binID);
        if (bin == null) return false;

        return binExists(bin, name);
    }

    public Bin getBin(String binID) {
        if (!binExists(binID)) return null;

        return (Bin)fileDB.get(binID);
    }

    public DBFile getFile(Bin bin, String name) {
        for (final DBFile dbFile : bin.files) {
            if (dbFile.name.equals(name)) {
                return dbFile;
            }
        }
        return null;
    }

    public DBFile getFile(String binID, String name) {
        Bin bin = getBin(binID);
        if (bin == null) return null;

        return getFile(bin, name);
    }

    public Bin removeInternal(Bin bin, String name) {
        Iterator<DBFile> iterator = bin.files.iterator();
        DBFile toRemove = null;
        while (iterator.hasNext()) {
            DBFile dbFile = iterator.next();
            if (dbFile.name.equals(name)) {
                File file = new File(dbFile.localPath);
                file.delete();
                toRemove = dbFile;
            }
        }
        bin.files.remove(toRemove);
        return bin;
    }

    public void remove(String binID, String name) {
        Bin bin = getBin(binID);
        if (bin == null) throw new RuntimeException("Bin not found.");

        fileDB.put(binID, removeInternal(bin, name));
        // removeInternal(bin, name);
    }

    public void removeBin(String binID) {
        Bin bin = getBin(binID);
        if (bin == null) throw new RuntimeException("Bin not found.");

        for (final DBFile dbFile : bin.files) {
            File file = new File(dbFile.localPath);
            file.delete();
        }

        fileDB.remove(binID);
    }
}
