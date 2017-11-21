import javafx.util.Pair;
import model.Vehicle;
import model.VehicleType;

import java.util.List;
import java.util.UUID;

import static java.lang.Math.*;

/**
 * Created by DukeKan on 09.11.2017.
 */
public class MetaGroup {
    private List<Vehicle> vehicles;
    private List<Pair<Double, Double>> vehiclePrevPositions;
    private Pair<Integer, Integer> targetPosition;

    private UUID id;
    private VehicleType vehicleType;

    public MetaGroup(List<Vehicle> vehicles, List<Pair<Double, Double>> vehpos, UUID id, VehicleType vehicleType) {
        this.vehicles = vehicles;
        vehiclePrevPositions = vehpos;
        this.id = id;
        this.vehicleType = vehicleType;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public double getVehicleX() {
        return vehicles.stream().mapToDouble(veh -> veh.getX()).average().getAsDouble();
    }

    public double getVehicleY() {
        return vehicles.stream().mapToDouble(veh -> veh.getY()).average().getAsDouble();
    }

    public boolean isMoving() {
        double prevX = vehiclePrevPositions.stream().mapToDouble(pos -> pos.getKey()).average().getAsDouble();
        double prevY = vehiclePrevPositions.stream().mapToDouble(pos -> pos.getValue()).average().getAsDouble();

        double currX = vehicles.stream().mapToDouble(veh -> veh.getX()).average().getAsDouble();
        double currY = vehicles.stream().mapToDouble(veh -> veh.getY()).average().getAsDouble();

        return coordinatesNotEqual(prevX, currX, prevY, currY);
    }

    private boolean coordinatesNotEqual(double x1, double x2, double y1, double y2) {
        return Math.abs(x1 - x2) > 0.02 && Math.abs(y1 - y2) > 0.02;
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

    public Pair<Integer, Integer> getPositionInTime(double time) {
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

    public Pair<Integer, Integer> getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(Pair<Integer, Integer> targetPosition) {
        this.targetPosition = targetPosition;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public int distanceTo(MetaGroup enemyMetaGroup) {
        return (int) (pow((pow(this.getVehicleX() - enemyMetaGroup.getVehicleX(), 2) + pow(this.getVehicleY() - enemyMetaGroup.getVehicleY(), 2)), 0.5));
    }
}
