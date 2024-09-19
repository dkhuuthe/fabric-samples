/*
 * This source file was generated by the Gradle 'init' task
 */
package FL.App;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.grpc.Status;
import io.grpc.ManagedChannel;
import org.hyperledger.fabric.client.ChaincodeEvent;
import org.hyperledger.fabric.client.CloseableIterator;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayRuntimeException;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.client.SubmitException;
import org.hyperledger.fabric.client.GatewayException;


import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import org.json.JSONObject;


import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;



import py4j.GatewayServer;


public class App implements AutoCloseable {
    private static final String channelName = "mychannel";
	private static final String chaincodeName = "FLchaincode";
    
    private static String sessionID = "session" + Instant.now().toEpochMilli();
    private static String clientID;
    private static String update;
    
    
    private static GatewayServer server;
    private static ManagedChannel grpcChannel;

    private final Network network;
	private final Contract contract;

    
    
    
    public String getGreeting(String para) {
        System.out.println(para);
        return para;
    }
    
    public static void main(String[] args) throws Exception{
        System.out.println("App is running");
        Init();
    }

    public App(final Gateway gateway) {
		network = gateway.getNetwork(channelName);
		contract = network.getContract(chaincodeName);
	}

    private static void Init() throws Exception {
        App.grpcChannel = Connections.newGrpcConnection();
		var builder = Gateway.newInstance()
				.identity(Connections.newIdentity())
				.signer(Connections.newSigner())
				.connection(App.grpcChannel)
				.evaluateOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
				.endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
				.submitOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
				.commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));
        var gateway = builder.connect();
        var mapp = new App(gateway);

        /**
        var sid = mapp.register("a","b","c");
        System.out.println("\n*** sessionID "+ sid);
        String sessionid = (String) sid.get("message");
        var nc = mapp.send(sessionid,"a","b","cc");
        System.out.println("\n*** nc "+ nc);
        **/
        App.server = new GatewayServer(mapp);
        try{
            App.server.start();
        }finally{
            //App.grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);          
        }
    }

    private HashMap<String, Object> RegisterClient(final String cid, final String mid,final String update) throws EndorseException, SubmitException, CommitStatusException,IOException {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        System.out.println("\n*** RegisterclientID "+cid+" "+ mid);
        
        var commit = contract.newProposal("RegisterclientID")
				.addArguments(cid, mid , update)
				.build()
				.endorse()
                .submitAsync();
			
        
        var status = commit.getStatus();
        var encryptedByteArray = commit.getResult();
        String message = new String(encryptedByteArray, StandardCharsets.UTF_8);
        
		if (!status.isSuccessful()) {
            System.out.println("\n*** RegisterclientID "+ status.getCode());
			throw new RuntimeException("failed to commit transaction with status code " + status.getCode());
		}
        System.out.println("\n*** RegisterclientID committed successfully");
        ret.put("message", message);
        ret.put("block", status.getBlockNumber());
        return ret;
    }


    private String SendUpdate(final String sid, final String cid, final String mid,final String update) throws EndorseException, SubmitException, CommitStatusException, CommitException,IOException {
        
        System.out.println("\n*** UpdateclientID "+ sid+" "+cid+" "+ mid);
        /**var encryptedByteArray = contract.newProposal("UpdateclientID")
				.addArguments(sid, cid, mid , update)
				.build()
				.endorse()
                .submit();
        String message = new String(encryptedByteArray, StandardCharsets.UTF_8);
        **/
        
        
        var commit = contract.newProposal("UpdateclientID")
				.addArguments(sid, cid, mid , update)
				.build()
				.endorse()
                .submitAsync();
			
        
        var status = commit.getStatus();
        var encryptedByteArray = commit.getResult();
        String message = new String(encryptedByteArray, StandardCharsets.UTF_8);
        System.out.println("\n*** UpdateclientID "+ message);
		if (!status.isSuccessful()) {
            System.out.println("\n*** UpdateclientID "+ status.getCode());
			throw new RuntimeException("failed to commit transaction with status code " + status.getCode());
		}
        
        System.out.println("\n*** UpdateclientID committed successfully");
        
        return message;
    }

    private String GetUpdate(final String sid) throws EndorseException, SubmitException, CommitStatusException, CommitException,GatewayException {
        try {
            //var encryptedByteArray = contract.evaluateTransaction("GetclientID", cid);
            var encryptedByteArray = contract.newProposal("getUpdate")
				.addArguments(sid)
				.build()
				.evaluate();
            String message = new String(encryptedByteArray, StandardCharsets.UTF_8);
            return message;
        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }
        return null;
    }

    private String SendAggUpdate(final String sid, final String mid,final String update) throws EndorseException, SubmitException, CommitStatusException, CommitException,IOException {
        
        System.out.println("\n*** SendAggUpdate "+ sid+" "+ mid);
        /**var encryptedByteArray = contract.newProposal("UpdateclientID")
				.addArguments(sid, cid, mid , update)
				.build()
				.endorse()
                .submit();
        String message = new String(encryptedByteArray, StandardCharsets.UTF_8);
        **/
        
        
        var commit = contract.newProposal("setAggUpdate")
				.addArguments(sid, mid , update)
				.build()
				.endorse()
                .submitAsync();
			
        
        var status = commit.getStatus();
        var encryptedByteArray = commit.getResult();
        String message = new String(encryptedByteArray, StandardCharsets.UTF_8);
        System.out.println("\n*** SendAggUpdate "+ encryptedByteArray);
		if (!status.isSuccessful()) {
            System.out.println("\n*** SendAggUpdate "+ status.getCode());
			throw new RuntimeException("failed to commit transaction with status code " + status.getCode());
		}
        
        System.out.println("\n*** SendAggUpdate committed successfully");
        
        return message;
    }

    private String GetAggUpdate(final String sid, final String clientID, final String modelID) throws EndorseException, SubmitException, CommitStatusException, CommitException,GatewayException {
        try {
            //var encryptedByteArray = contract.evaluateTransaction("GetclientID", cid);
            var encryptedByteArray = contract.newProposal("getAggUpdate")
				.addArguments(sid,clientID, modelID)
				.build()
				.evaluate();
            String message = new String(encryptedByteArray, StandardCharsets.UTF_8);
            return message;
        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }
        return null;
    }

    private String SetResult(final String cid, final String mid,final String result) throws EndorseException, SubmitException, CommitStatusException, CommitException,IOException {
        
        System.out.println("\n*** SetResult "+ cid+" "+ mid);
        /**var encryptedByteArray = contract.newProposal("UpdateclientID")
				.addArguments(sid, cid, mid , update)
				.build()
				.endorse()
                .submit();
        String message = new String(encryptedByteArray, StandardCharsets.UTF_8);
        **/
        
        
        var commit = contract.newProposal("SetResult")
				.addArguments(cid, mid , result)
				.build()
				.endorse()
                .submitAsync();
			
        
        var status = commit.getStatus();
        var encryptedByteArray = commit.getResult();
        String message = new String(encryptedByteArray, StandardCharsets.UTF_8);
        System.out.println("\n*** SetResult "+ encryptedByteArray);
		if (!status.isSuccessful()) {
            System.out.println("\n*** SetResult "+ status.getCode());
			throw new RuntimeException("failed to commit transaction with status code " + status.getCode());
		}
        
        System.out.println("\n*** SetResult committed successfully");
        
        return message;
    }


    private String wait_for_event(final String sid, final String ucode) throws EndorseException, SubmitException, CommitStatusException, CommitException,GatewayException{

        var startBlock = 0;
        var request = network.newChaincodeEventsRequest(chaincodeName)
				.startBlock(startBlock)
				.build();
        String payload = null;
        try (var eventIter = request.getEvents()) {
            
			while (eventIter.hasNext()) {
                
				var event = eventIter.next();
				payload = new String(event.getPayload()); 
				if (event.getEventName().equals(sid) && ucode.equals(payload) ) {
                    System.out.println("\n<-- Chaincode event : " + event.getEventName() + " - " + payload);
					break;
				}
			}
		}
        return payload;
        
    }

    public void reset(){
        try{
            var commit = contract.newProposal("Reset")
				.build()
				.endorse()
                .submitAsync();
            var status = commit.getStatus();
            if (!status.isSuccessful()) {
                System.out.println("\n*** Reset "+ status.getCode());
    			throw new RuntimeException("failed to commit transaction with status code " + status.getCode());
    		}
        }catch(EndorseException | SubmitException | CommitStatusException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    public String set_result(final String cid, final String mid,final String result){
        try{
            var message = SetResult(cid, mid, result);
            return message;
        }catch(EndorseException | SubmitException | CommitStatusException | CommitException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }catch( IOException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    public String wait_for_agg(final String sid, final String ucode){
        try{
            var message = wait_for_event(sid, ucode);
            return message;
        }catch(EndorseException | SubmitException | CommitStatusException | CommitException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }catch( GatewayException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }


    public String getAgg(final String sid, final String clientID, final String modelID) {
        try{
            var message = GetAggUpdate(sid, clientID, modelID);
            return message;
        }catch(EndorseException | SubmitException | CommitStatusException | CommitException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }catch( GatewayException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    public String sendAgg(final String sid, final String mid,final String update){
        try{
            var message = SendAggUpdate(sid, mid, update);
            return message;
        }catch(EndorseException | SubmitException | CommitStatusException | CommitException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }catch( IOException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }


    public String get(final String sid){
        try{
            var message = GetUpdate(sid);
            return message;
        }catch(EndorseException | SubmitException | CommitStatusException | CommitException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }catch( GatewayException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    public String send(final String sid, final String cid, final String mid, final String update){
        try{
            var message = SendUpdate(sid, cid, mid, update);
            return message;
        }catch(EndorseException | SubmitException | CommitStatusException | CommitException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }catch( IOException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    public Map<String, Object> register(final String cid, final String mid, final String update){
        try{
            Map<String, Object> message = RegisterClient(cid, mid, update);
            return message;
        }catch(EndorseException | SubmitException | CommitStatusException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }catch( IOException ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }



    public String send_update(final String value,String sID){
        if (sessionID.equals(sID)){
            update = value;
            return update;
        }else{
            return null;    
        }
        
    }

    public String receive_update(final String sID){
        if (sessionID.equals(sID)){
            return update;
        }else{
            return null;    
        }
    }


    
    
    

    @Override
	public void close() throws Exception {
        //pyCon.shutdown();
        App.server.shutdown();
        App.grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);  
	}
}
