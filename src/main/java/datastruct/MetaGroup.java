package datastruct;

import model.Vehicle;
import model.VehicleType;

import java.util.List;

import static java.lang.Math.*;

/**
 * Created by DukeKan on 09.11.2017.
 */
public class MetaGroup {
    private List<Vehicle> vehicles;

    public MetaGroup(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public int getTimeToPoint(int x, int y) {
        return (int) vehicles.stream().mapToDouble(veh -> getDist(veh, x, y) / veh.getMaxSpeed()).average().getAsDouble();
    }

    private double getDist(Vehicle veh, int x, int y) {
        return pow((pow(veh.getX() - x, 2) + pow(veh.getY() - y, 2)), 0.5);
    }

}
