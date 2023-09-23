package pl.nadwey.NadBin;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Bin implements Serializable {
    public List<DBFile> files = new ArrayList<>();
    public LocalDateTime creationDate;

    public boolean isLocked = false;
}
