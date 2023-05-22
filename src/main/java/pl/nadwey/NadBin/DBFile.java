package pl.nadwey.NadBin;

import java.io.Serializable;

public class DBFile implements Serializable {
    public DBFile(String name, String localPath, long size) {
        this.name = name;
        this.localPath = localPath;
        this.size = size;
    }

    public String name;
    public String localPath;
    public long size;
}
