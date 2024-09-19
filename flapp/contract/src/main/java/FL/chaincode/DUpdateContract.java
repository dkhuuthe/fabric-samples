/*
 * DUpdateContract
 * temp/CODE/chaincode/app/src/main/java/FL/chaincode/DUpdateContract.java
 */

package FL.chaincode;

import static java.nio.charset.StandardCharsets.UTF_8;
//import static java.nio.charset.StandardCharsets.US_ASCII;


import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;

import java.time.Instant;
import java.util.Map;
import java.util.LinkedList;
import java.util.Queue;



import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.util.logging.*;


import java.util.concurrent.Semaphore;

@Contract(
        name = "DUpdateContract-java",
        info = @Info(
                title = "DUpdateContract",
                description = "FL comminication",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "DUpdateContract@example.com",
                        name = "DUpdateContract Development Team",
                        url = "https://hyperledger.example.com")))

@Default
public final class DUpdateContract implements ContractInterface {
    static final String IMPLICIT_COLLECTION_NAME_PREFIX = "_implicit_org_";
    static final String PRIVATE_PROPS_KEY = "client_properties";
    static Queue<String> queueClient = new LinkedList<>();
    static Integer nClients = 0;
    static Integer nClientUpdates = 0;
    static String sessionID = "session" + Instant.now().toEpochMilli();
    static String status = null;

    // create a Semaphore instance that makes it so only 1 thread can access resource at a time
    private static Semaphore mutex = new Semaphore(1);

    private static Logger logger = Logger.getLogger("Logger DUpdate");; 


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void Reset(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        //stub.putState("status", "start_round".getBytes());
        try{
                mutex.acquire();
                sessionID = "session" + Instant.now().toEpochMilli();
                nClients = 0;
                nClientUpdates = 0;
                queueClient.clear();
                status = "start_round";
                
            }catch (Exception e) {
                e.printStackTrace();
            }finally{
                mutex.release();
        }
    }
    
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String RegisterclientID(final Context ctx, final String clientID, final String modelID,  final String update) {
        ChaincodeStub stub = ctx.getStub();

        Boolean init_flag = false;
        //byte[] status = stub.getState("status");
        if (status == null) {
            init_flag = true;
        }else {
            //var message = new String(status,UTF_8);
            if ("end_round".equals(status)){
                init_flag = true;
                //stub.delState("status");
            }
        }

        if(init_flag){
            try{
                mutex.acquire();
                sessionID = "session" + Instant.now().toEpochMilli();
                nClients = 0;
                nClientUpdates = 0;
                queueClient.clear();
                status = "start_round";
                init_flag = false;
            }catch (Exception e) {
                e.printStackTrace();
            }finally{
                mutex.release();
            }
        }
        
        // input validations
        String errorMessage = null;
        if (clientID == null || clientID.equals("")) {
            errorMessage = String.format("Empty input: clientID");
        }
        if (modelID == null || modelID.equals("")) {
            errorMessage = String.format("Empty input: modelID");
        }

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, DUpdateContractErrors.INCOMPLETE_INPUT.toString());
        }
        // Check if asset already exists
        byte[] dUpdateJSON = ctx.getStub().getState(clientID);
        if (dUpdateJSON != null && dUpdateJSON.length > 0) {
            errorMessage = String.format("Client %s already exists", clientID);
            System.err.println(errorMessage);
            //throw new ChaincodeException(errorMessage, DUpdateContractErrors.CLIENT_ALREADY_EXISTS.toString());
            return sessionID;
        }else{
            try{
                mutex.acquire();
                nClients+=1;
                
            }catch (Exception e) {
                e.printStackTrace();
            }finally{
                mutex.release();
            }
        }


        DUpdate session = new DUpdate(sessionID, modelID, update);

        savePrivateData(ctx, sessionID);
        dUpdateJSON = session.serialize();
        //System.out.printf("RegisterclientID: ID %s\n", clientID);
        logger.info("RegisterclientID: ID " +clientID+ "\n" );
        logger.info("RegisterclientID: ID " +nClients+ "\n" );
        //logger.log(Level.INFO, "trouble sneezing", ex);
        stub.putState(clientID, dUpdateJSON);

        return sessionID;
    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Integer UpdateclientID(final Context ctx, final String sid, final String clientID, final String modelID,  final String update) {
        ChaincodeStub stub = ctx.getStub();
        // input validations
        String errorMessage = null;

        logger.info("UpdateclientID:  " +clientID +" sid "+ sid+" sessionID "+ sessionID+  "\n" );
        
        if (sid == null || !sid.equals(sessionID)){
            errorMessage = String.format("bad sessionID");
        }
        
        if (clientID == null || clientID.equals("")) {
            errorMessage = String.format("Empty input: clientID");
        }
        if (modelID == null || modelID.equals("")) {
            errorMessage = String.format("Empty input: modelID");
        }

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, DUpdateContractErrors.INCOMPLETE_INPUT.toString());
        }
        // Check if asset already exists
        byte[] dUpdateJSON = stub.getState(clientID);

        if (dUpdateJSON == null) {
            errorMessage = String.format("Client %s does not exist", clientID);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, DUpdateContractErrors.CLIENT_NOT_FOUND.toString());
        }
        
        DUpdate client_session = new DUpdate(sid,modelID,update);


        savePrivateData(ctx, sid);
        dUpdateJSON = client_session.serialize();
        System.out.printf("UpdateclientID: ID %s\n", clientID);
        stub.putState(clientID, dUpdateJSON);
        Boolean retflag = false;
        try{
            mutex.acquire();
            if (!queueClient.contains(clientID)){
                queueClient.add(clientID);
                nClientUpdates =queueClient.size();
            }
            
            retflag = nClientUpdates>=nClients;
        }catch (Exception e) {
            e.printStackTrace();
        }finally{
            mutex.release();
        }
        
        if(retflag){
            String updateCode = String.valueOf(nClientUpdates);
            stub.setEvent(sessionID, updateCode.getBytes());
            return nClientUpdates;
        }
        
        logger.info("UpdateclientID:  " +clientID +" sid "+ sid+" nClientUpdates "+ nClientUpdates +" queueClient "+ queueClient+  "\n" );
        return  -1;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getUpdate(final Context ctx, final String sid) {
        System.out.printf("getUpdate");
        String cid = null;
         try{
            mutex.acquire();
            if (!queueClient.isEmpty()){
                cid = queueClient.remove();
                nClientUpdates =queueClient.size();
            }
            
        }catch (Exception e) {
            e.printStackTrace();
        }finally{
                mutex.release();
        }

        if (cid!=null){
            DUpdate client_session = getState(ctx, cid);
            if (sid == null || !sid.equals(client_session.getsessionID())){
                String errorMessage = String.format("bad sessionID");
                throw new ChaincodeException(errorMessage, DUpdateContractErrors.INVALID_ACCESS.toString());
            }
            return client_session.getupdate();    
        }
        return null;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String setAggUpdate(final Context ctx, final String sid, final String modelID, final String update) {
        ChaincodeStub stub = ctx.getStub();
        System.out.printf("setAggUpdate");
        // input validations
        String errorMessage = null;

        logger.info("setAggUpdate: sid "+ sid +  "\n" );
        
        if (sid == null || !sid.equals(sessionID)){
            errorMessage = String.format("bad sessionID");
        }
        
        if (update == null || update.equals("")) {
            errorMessage = String.format("Empty input: update");
        }

        if (modelID == null || modelID.equals("")) {
            errorMessage = String.format("Empty input: modelID");
        }

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, DUpdateContractErrors.INCOMPLETE_INPUT.toString());
        }

        // Check if asset already exists
        

        DUpdate session = new DUpdate(sid, modelID, update);

        savePrivateData(ctx, sid);
        byte[] dUpdateJSON = session.serialize();
        
        logger.info("setAggUpdate: sid " +sid+ "\n" );
        
        stub.putState(sid, dUpdateJSON);

        String updateCode = String.valueOf(-1);
        stub.setEvent(sessionID, updateCode.getBytes());
        return sessionID;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAggUpdate(final Context ctx, final String sid, final String clientID, final String modelID) {
        ChaincodeStub stub = ctx.getStub();
        System.out.printf("getAggUpdate");
        String errorMessage = null;
        logger.info("getAggUpdate: sid "+ sid +  "\n" );
        
        if (sid == null || !sid.equals(sessionID)){
            errorMessage = String.format("bad sessionID");
        }
        
        if (modelID == null || modelID.equals("")) {
            errorMessage = String.format("Empty input: modelID");
        }

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, DUpdateContractErrors.INCOMPLETE_INPUT.toString());
        }

        System.out.printf("getAggUpdate: verify agg update of session %s exists\n", sid);
        DUpdate client = getState(ctx, sid);

        DUpdate client_session = getState(ctx, sid);

        try{
            mutex.acquire();
            if (!queueClient.contains(clientID) && nClientUpdates < nClients){
                queueClient.add(clientID);
                nClientUpdates =queueClient.size();
            }
            
            
        }catch (Exception e) {
            e.printStackTrace();
        }finally{
                mutex.release();
        }
        
        return client_session.getupdate();    
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String SetResult(final Context ctx, final String clientID, final String modelID, final String result) {
        ChaincodeStub stub = ctx.getStub();
        
        try{
            mutex.acquire();
            if (!queueClient.isEmpty()){
                queueClient.remove();
                nClientUpdates =queueClient.size();
            }
            
        }catch (Exception e) {
            e.printStackTrace();
        }finally{
                mutex.release();
        }
        
        String updateCode = "end_round";
        if(nClientUpdates <= 0){
            
            stub.setEvent(sessionID, updateCode.getBytes());
            //stub.putState("status", updateCode.getBytes());
            
            try{
                mutex.acquire();
                status = updateCode;                
            }catch (Exception e) {
                e.printStackTrace();
            }finally{
                mutex.release();
            }
        }
        var client = stub.getState(clientID);
        if (client!=null){
            stub.delState(clientID);
        }
        
        return updateCode;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void DeleteClient(final Context ctx, final String cid) {
        ChaincodeStub stub = ctx.getStub();
        System.out.printf("DeleteClient: verify cid %s exists\n", cid);
        DUpdate client = getState(ctx, cid);

        System.out.printf(" DeleteClient:  cid %s\n", cid);
        // delete private details of asset
        //removePrivateData(ctx, assetID);
        stub.delState(cid);         // delete the key from Statedb
    }
    
    private DUpdate getState(final Context ctx, final String cID) {
        byte[] DUpdateJSON = ctx.getStub().getState(cID);
        if (DUpdateJSON == null || DUpdateJSON.length == 0) {
            String errorMessage = String.format("ID %s does not exist", cID);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, DUpdateContractErrors.CLIENT_NOT_FOUND.toString());
        }

        try {
            DUpdate asset = DUpdate.deserialize(DUpdateJSON);
            return asset;
        } catch (Exception e) {
            throw new ChaincodeException("Deserialize error: " + e.getMessage(), DUpdateContractErrors.DATA_ERROR.toString());
        }
    }

    private String readPrivateData(final Context ctx, final String cKey) {
        String peerMSPID = ctx.getStub().getMspId();
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String implicitCollectionName = getCollectionName(ctx);
        String privData = null;
        // only if ClientOrgMatchesPeerOrg
        if (peerMSPID.equals(clientMSPID)) {
            System.out.printf(" ReadPrivateData from collection %s, ID %s\n", implicitCollectionName, cKey);
            byte[] propJSON = ctx.getStub().getPrivateData(implicitCollectionName, cKey);

            if (propJSON != null && propJSON.length > 0) {
                privData = new String(propJSON, UTF_8);
            }
        }
        return privData;
    }

    private void savePrivateData(final Context ctx, final String cKey) {
        String peerMSPID = ctx.getStub().getMspId();
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String implicitCollectionName = getCollectionName(ctx);

        if (peerMSPID.equals(clientMSPID)) {
            Map<String, byte[]> transientMap = ctx.getStub().getTransient();
            if (transientMap != null && transientMap.containsKey(PRIVATE_PROPS_KEY)) {
                byte[] transientAssetJSON = transientMap.get(PRIVATE_PROPS_KEY);

                System.out.printf("client's PrivateData Put in collection %s, ID %s\n", implicitCollectionName, cKey);
                ctx.getStub().putPrivateData(implicitCollectionName, cKey, transientAssetJSON);
            }
        }
    }

    private void removePrivateData(final Context ctx, final String cKey) {
        String peerMSPID = ctx.getStub().getMspId();
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String implicitCollectionName = getCollectionName(ctx);

        if (peerMSPID.equals(clientMSPID)) {
            System.out.printf("PrivateData Delete from collection %s, ID %s\n", implicitCollectionName, cKey);
            ctx.getStub().delPrivateData(implicitCollectionName, cKey);
        }
    }

    // Return the implicit collection name, to use for private property persistance
    private String getCollectionName(final Context ctx) {
        // Get the MSP ID of submitting client identity
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String collectionName = IMPLICIT_COLLECTION_NAME_PREFIX  + clientMSPID;
        return collectionName;
    }

    private enum DUpdateContractErrors {
        INCOMPLETE_INPUT,
        INVALID_ACCESS,
        CLIENT_NOT_FOUND,
        CLIENT_ALREADY_EXISTS,
        DATA_ERROR
    }
}
