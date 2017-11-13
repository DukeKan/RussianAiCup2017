import javafx.util.Pair;
import model.Vehicle;

/**
 * Created by DukeKan on 13.11.2017.
 */
public class NuclearInfo {
    private Pair<Double, Double> coordinates;
    private Vehicle vehicle;

    public Pair<Double, Double> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Pair<Double, Double> coordinates) {
        this.coordinates = coordinates;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}
