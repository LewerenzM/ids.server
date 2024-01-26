package org.icatproject.ids.v3.model;
import org.icatproject.ids.plugin.DfInfo;

/**
 * Contains information about a Datafile. Replaces DsInfo in v3
 * May should implement DfInfo interface
 */
public class DataFileInfo extends DataInfoBase implements Comparable<DataFileInfo>, DfInfo {

    protected String createId;
    protected String modId; 
    protected Long datasId;

    public DataFileInfo(Long id, String name, String location, String createId, String modId, Long datasId) {
        super(id, name, location);

        this.createId = createId;
        this.modId = modId;
        this.datasId = datasId;
    }

    public String getCreateId() {
        return this.createId;
    }

    public String getModId() {
        return this.modId;
    }

    public Long getDsId() {
        return this.datasId;
    }

    @Override
    public String toString() {
        return this.location;
    }

    @Override
    public int compareTo(DataFileInfo o) {
        if (this.id > o.getId()) {
            return 1;
        }
        if (this.id < o.getId()) {
            return -1;
        }
        return 0;
    }

    // implementing DfInfo
    @Override
    public Long getDfId() { return this.getId(); }
    @Override
    public String getDfLocation() { return this.getLocation(); }
    @Override
    public String getDfName() { return this.getName(); }
}