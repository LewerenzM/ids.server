package org.icatproject.ids.v3.model;


/**
 * A Base class for Data objct types. like Datasets or Datafiles
 */
public abstract class DataInfoBase {

    protected Long id;
    protected String name;
    protected String location;

    protected DataInfoBase(Long id, String name, String location){
        this.name = name;
        this.id = id;
        this.location = location;
    }

    @Override
    public abstract String toString();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public int hashCode() {
        return (int) (this.id ^ (this.id >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        return this.id == ((DataInfoBase) obj).getId();
    }
    
}