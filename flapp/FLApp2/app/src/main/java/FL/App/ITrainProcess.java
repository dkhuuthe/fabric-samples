package FL.App;
import java.util.ArrayList;

public interface ITrainProcess {
    public String init_model(String config_file, String data_path, String output_path);
    public String train_and_send_Update(Integer nround,Integer nepochs);
    public void receive_Update(String value);
    public String agg_model(String a, String b,Double d);
    public String test_model();
    public void shutdown();
}