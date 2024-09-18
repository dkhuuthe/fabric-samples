package FL.App;
import org.apache.commons.io.IOUtils;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import py4j.GatewayServer;


public class pyConnection {
    List<ITrainProcess> listeners = new ArrayList<ITrainProcess>();
    ITrainProcess train;
    GatewayServer server;

    public void registerListener(ITrainProcess listener) {
        listeners.add(listener);
    }

    public pyConnection() {
		//GatewayServer.turnLoggingOff();
        //server = new GatewayServer(null, 25334,0,0);
        server = new GatewayServer();
        server.start();
        train = (ITrainProcess) server.getPythonServerEntryPoint(new Class[] { ITrainProcess.class });
	}

    public String init_model(String config_file, String data_path, String output_path) {
        try{
            String ret = train.init_model(config_file,data_path,output_path);
            return ret;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public void receive_Update(String value){
        try{
            train.receive_Update(value);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public String train_and_send_Update(Integer nround,Integer nepochs){
        try{
            return train.train_and_send_Update(nround, nepochs);
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public String agg_model(String a, String b, Double d){
        String ret = train.agg_model(a,b,d);
        return ret;
    }

    public String test_model(){
        
        try{
            var ret = train.test_model();
        //server.shutdown();
            return ret;
        }catch (Exception e) {
              e.printStackTrace();
          }
        return null;
    }

    public void shutdown(){
        try{
            train.shutdown();
            server.shutdown();
        }catch (Exception e) {
              //e.printStackTrace();
            
          }
    }

    @Override
    public int hashCode() {
        return Objects.hash(train);
    }
}