import javafx.util.Pair;
import model.Player;
import model.Vehicle;

/**
 * Created by DukeKan on 09.11.2017.
 */
public class PlayerExt {
    public static Player me;
    public static Player enemy;

    private Player player;
    private WorldExt worldExt;
    private int defaultTicsUntilNuclearBomb = 1200;
    private int ticsUntilNuclearBomb = defaultTicsUntilNuclearBomb;
    private Vehicle nuclearBombVehicle;
    private Runnable nuclearVehicleDeadListener;
    private Pair<Double, Double> nuclearCoordinates;

    public void tick() {
        ticsUntilNuclearBomb--;
        if (ticsUntilNuclearBomb > 0 && ticsUntilNuclearBomb % 300 == 0) {
            System.out.println("Nuclear bomb left: " + ticsUntilNuclearBomb);
        }
        if (nuclearBombAvailable()) {
            //System.out.println("Nuclear available!!!");
            if (!hasNuclearVehicle() && nuclearVehicleDeadListener != null) {
                nuclearVehicleDeadListener.run();
            }
        }
    }

    public Pair<Double, Double> getNuclearCoordinates() {
        return nuclearCoordinates;
    }

    public void setNuclearCoordinates(Pair<Double, Double> nuclearCoordinates) {
        this.nuclearCoordinates = nuclearCoordinates;
    }

    public Vehicle getNuclearBombVehicle() {
        return nuclearBombVehicle;
    }

    public void setNuclearBombVehicle(Vehicle nuclearBombVehicle) {
        System.out.println("Set new nuclear vehicle");
        this.nuclearBombVehicle = nuclearBombVehicle;
    }

    public void dropNuclearBomb() {
        ticsUntilNuclearBomb = defaultTicsUntilNuclearBomb;
        nuclearBombVehicle = null;
    }

    public boolean nuclearBombAvailable() {
        return ticsUntilNuclearBomb <= 0;
    }

    public boolean hasNuclearVehicle(){
        return nuclearBombVehicle != null && worldExt.getVehicleById(nuclearBombVehicle.getId()) != null;
    }

    public Runnable getNuclearVehicleDeadListener() {
        return nuclearVehicleDeadListener;
    }

    public void setNuclearVehicleDeadListener(Runnable nuclearVehicleDeadListener) {
        this.nuclearVehicleDeadListener = nuclearVehicleDeadListener;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public int getTicsUntilNuclearBomb() {
        return ticsUntilNuclearBomb;
    }

    public void setTicsUntilNuclearBomb(int ticsUntilNuclearBomb) {
        this.ticsUntilNuclearBomb = ticsUntilNuclearBomb;
    }

    public PlayerExt(Player player, WorldExt worldExt) {
        this.player = player;
        this.worldExt = worldExt;
    }

    public enum Ownership {
        ANY,

        MY,

        ENEMY
    }
}
