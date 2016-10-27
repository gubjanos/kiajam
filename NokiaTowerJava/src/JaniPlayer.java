public class JaniPlayer extends Player {
  private int time;
  private Map[] map;

  public JaniPlayer(int maxTime) {
    map = new Map[maxTime];
  }

  public static void makeMove(TPlayer player) {
    System.out.println(player.myTime);
    //if (player.headerIni != null && player.myTime == 1)
  }
}
