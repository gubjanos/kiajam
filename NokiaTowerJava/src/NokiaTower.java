import java.io.FileNotFoundException;

public class NokiaTower {

    public static void main(String[] args) throws FileNotFoundException {
    
    	if (args.length < 1) {
    		System.out.println("ERROR: missing parameters!");
    		System.out.println("Usage: program.exe C=n");
    		System.out.println("       n: 0..3");
            
        }
        else {

            Comm.initComm();

            TPlayer player = new TPlayer();

            player.ID = 1 + (args[0].charAt(2) - '0');
            System.out.println("Player ID: " + player.ID);

            if (args.length >= 2) {
                player.scriptName = args[1];
                player.loadScript();
                System.out.println("Script Name: " + player.scriptName);
            }

            Comm.readIniData(player);
            player.init();
            Comm.writeData(player);

            while (Comm.readData(player) != 0) {
                JaniPlayer.makeMove(player);
                Comm.writeData(player);
            }

        }
    	
    }

}
