import javafx.util.Pair;
import model.Vehicle;
import model.VehicleType;

import java.util.List;

import static java.lang.Math.*;

/**
 * Created by DukeKan on 09.11.2017.
 */
public class MetaGroup {
    private List<Vehicle> vehicles;
    private List<Pair<Double, Double>> vehiclePrevPositions;

    public MetaGroup(List<Vehicle> vehicles, List<Pair<Double, Double>> vehpos) {
        this.vehicles = vehicles;
        vehiclePrevPositions = vehpos;
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

    public int getTimeToPoint(int distance) {
        return (int) vehicles.stream().mapToDouble(veh -> distance / veh.getMaxSpeed()).average().getAsDouble();
    }

    private double getDist(Vehicle veh, int x, int y) {
        return pow((pow(veh.getX() - x, 2) + pow(veh.getY() - y, 2)), 0.5);
    }

    public Pair<Integer, Integer> getPositionInTime(int time) {
        double avSpeed = vehicles.stream().mapToDouble(veh -> veh.getMaxSpeed()).max().orElse(0);
        double avX = vehicles.stream().mapToDouble(veh -> veh.getX()).average().orElse(512);
        double avY = vehicles.stream().mapToDouble(veh -> veh.getY()).average().orElse(512);
        double prevAvX = vehiclePrevPositions.stream().mapToDouble(pair -> pair.getKey()).average().orElse(512);
        double prevAvY = vehiclePrevPositions.stream().mapToDouble(pair -> pair.getValue()).average().orElse(512);

        double diffX = avX - prevAvX;
        double diffY = avY - prevAvY;

        double nextX = avX + diffX * avSpeed * time;
        double nextY = avY + diffY * avSpeed * time;

        if (nextX < 0) {
            nextX = 0;
        }
        if (nextY < 0) {
            nextY = 0;
        }

        if (nextX > 1024) {
            nextX = 1024;
        }
        if (nextY > 1024) {
            nextY = 1024;
        }

        return new Pair<>((int) nextX, (int) nextY);
    }
}
