package pl.nadwey.NadBin;

import org.mapdb.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.NavigableSet;

public class DBManager {
    BTreeMap fileDB;
    DB dbMaker;

    public DBManager(String dbPath) {
        dbMaker = DBMaker.fileDB(dbPath).make();
        try {
            fileDB = dbMaker.treeMap("btree", Serializer.STRING, Serializer.JAVA).createOrOpen();
        }
        catch (DBException.SerializationError ex) {
            System.err.println("Existing database is incompatible with the current one. Please remove the \"files.db\" file. Maybe I will make a tool for converting...");
        }
    }

    public void close() {
        if (!fileDB.isClosed()) fileDB.close();
        if (!dbMaker.isClosed()) dbMaker.close();
    }

    public void addFileToBin(String binID, String name, String localPath, long size) {
        Bin bin = getBin(binID);
        if (bin == null) {
            bin = new Bin();
            bin.creationDate = LocalDateTime.now();
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

    public void removeExpiredBins(Long maxBinAge) {
        NavigableSet<String> binIDs = fileDB.getKeys();
        int removedBins = 0;

        for (final String binID : binIDs) {
            Bin bin = getBin(binID);
            if (bin == null) continue;

            LocalDateTime minimalCreationDate = LocalDateTime.now().minus(maxBinAge, ChronoUnit.MINUTES);
            if (bin.creationDate.isBefore(minimalCreationDate)) {
                removeBin(binID);
                removedBins++;
            }
        }

        if (removedBins > 0) {
            System.out.println("Removed " + removedBins + " expired bins");
        }
    }
}
