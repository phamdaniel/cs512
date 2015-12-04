// -------------------------------// Adapted from Kevin T. Manley// CSE 593// -------------------------------package server;import java.io.Serializable;import java.util.*;// A specialization of Hashtable with some extra diagnostics.public class RMHashtable extends Hashtable {    RMHashtable() {      super();    }    public String toString() {        String s = "RMHashtable { \n";        Object key = null;        for (Enumeration e = keys(); e.hasMoreElements();) {            key = e.nextElement();            String value = (String) get(key);            s = s + "  [key = " + key + "] " + value + "\n";        }        s = s + "}";        return s;    }    public RMHashtable clone() {        Trace.info("Cloning");        RMHashtable ret = new RMHashtable();        Object key = null;        for (Enumeration e = keys(); e.hasMoreElements();) {            key = e.nextElement();            ReservedItem value = (ReservedItem)get(key);            ret.put(key, value.getClone());        }        return ret;    }    public void dump() {        System.out.println(toString());    }}