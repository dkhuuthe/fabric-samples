/*
 * DUpdate stores data sent by clients
 */
package FL.chaincode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;


import org.json.JSONObject;

@DataType()
public final class DUpdate {
    @Property()
    private final String sessionID;
    @Property()
    private  String modelID;
    @Property()
    private  String update;

    public DUpdate(final String sessionID, final String modelID, final String update) {
        this.sessionID = sessionID;
        this.modelID = modelID;
        this.update = update;
    }

    public String getsessionID() {
        return this.sessionID;
    }

    public String getmodelID() {
        return this.modelID;
    }

    public String getupdate() {
        return this.update;
    }

    public void setupdateID(final String value) {
        this.update = value;
    }

    public void setmodelID(final String value) {
        this.modelID = value;
    }

    public String serialize(final String privateProps) {
        Map<String, Object> tMap = new HashMap();
        tMap.put("sessionID", this.sessionID);
        tMap.put("modelID",  this.modelID);
        tMap.put("update",  this.update);
        if (privateProps != null && privateProps.length() > 0) {
            tMap.put("client_properties", new JSONObject(privateProps));
        }
        return new JSONObject(tMap).toString();
    }
    
    // Serialize asset with private properties: modelID
    public byte[] serialize() {
        return serialize(null).getBytes(UTF_8);
    }
    
    public static DUpdate deserialize(final String DUpdateJSON) {
        JSONObject json = new JSONObject(DUpdateJSON);
        Map<String, Object> tMap = json.toMap();
        final String sessionid = (String) tMap.get("sessionID");
        final String modelid = (String) tMap.get("modelID");
        final String update = (String) tMap.get("update");
        return new DUpdate(sessionid, modelid, update);
    }
    
    public static DUpdate deserialize(final byte[] DUpdateJSON) {
        return deserialize(new String(DUpdateJSON, UTF_8));
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        DUpdate other = (DUpdate) obj;

        return Objects.deepEquals(
                new String[]{getsessionID(), getmodelID()},
                new String[]{other.getsessionID(), other.getmodelID()});
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getsessionID(), getmodelID());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                + " [sessionID=" + sessionID + ", modelID=" + modelID  + "]";
    }
}
