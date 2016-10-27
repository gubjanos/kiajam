import com.sun.jna.Library;
import com.sun.jna.Native;

public interface CommLibrary extends Library {
    CommLibrary INSTANCE = (CommLibrary) Native.loadLibrary("comm.dll", CommLibrary.class);
    int init_proc();
    int read_data_proc(byte[] bytes);
    int write_data_proc(byte[] bytes);
}