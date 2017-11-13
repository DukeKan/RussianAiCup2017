import model.Player;

/**
 * Created by DukeKan on 09.11.2017.
 */
public class PlayerExt {
    public static Player me;

    private Player player;
    private int defaultTicsUntilNuclearBomb = 1200;
    private int ticsUntilNuclearBomb = defaultTicsUntilNuclearBomb;

    public void tick() {
        ticsUntilNuclearBomb--;
    }

    //todo отслеживать, не умер ли наводящий и сбрасывать
    public void dropNuclearTimer() {
        ticsUntilNuclearBomb = defaultTicsUntilNuclearBomb;
    }

    public boolean nuclearBombAvailable() {
        return ticsUntilNuclearBomb <= 0;
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

    public PlayerExt(Player player) {
        this.player = player;
    }

    public enum Ownership {
        ANY,

        MY,

        ENEMY
    }
}
