package datastruct;

import datastruct.PlayerExt.Ownership;
import model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static datastruct.PlayerExt.Ownership.*;

/**
 * Created by DukeKan on 08.11.2017.
 */
public class WorldExt {
    private World world;
    private MetaCell[][] metaCells;
    private final Map<Long, Vehicle> vehicleById = new HashMap<>();
    private final Map<Long, Integer> updateTickByVehicleId = new HashMap<>();

    public WorldExt(World world) {
        this.world = world;
    }

    public double getWidth() {
        return world.getWidth();
    }

    public WeatherType[][] getWeather() {
        return world.getWeatherByCellXY();
    }

    public TerrainType[][] getTerrain() {
        return world.getTerrainByCellXY();
    }

    public Facility[] getFacilities() {
        return world.getFacilities();
    }

    public double getHeight() {
        return world.getHeight();
    }

    public void separateByMetaCells(int size) {
        //todo обработать ситуацию, когда не делится нацело
        int width = (int) getWidth() / size;
        int height = (int) getHeight() / size;
        int i = 0;
        int j = 0;
        metaCells = new MetaCell[height][width];
        for (int x = 0; x < getHeight(); x = size + i * size, i++) {
            for (int y = 0; y < getWidth(); y = size + j * size, j++) {

//                TerrainType[][] terrainTypes = new TerrainType[size][size];
//                for (int smallX = x; smallX < x + size; smallX ++) {
//                    for (int smallY = y; smallY < y + size; smallY ++) {
//                        terrainTypes[smallX-x][smallY-y] = getTerrain()[x + smallX][y + smallY];
//                    }
//                }

                // инкапсулируем информацию о метаклетке
                MetaCell metaCell = new MetaCell(x, y, i, j, size, null);
                metaCells[i][j] = metaCell;
            }
            j = 0;
        }
    }

    public void tick(World world){
        for (Vehicle vehicle : world.getNewVehicles()) {
            vehicleById.put(vehicle.getId(), vehicle);
            updateTickByVehicleId.put(vehicle.getId(), world.getTickIndex());
        }

        for (VehicleUpdate vehicleUpdate : world.getVehicleUpdates()) {
            long vehicleId = vehicleUpdate.getId();

            if (vehicleUpdate.getDurability() == 0) {
                vehicleById.remove(vehicleId);
                updateTickByVehicleId.remove(vehicleId);
            } else {
                vehicleById.put(vehicleId, new Vehicle(vehicleById.get(vehicleId), vehicleUpdate));
                updateTickByVehicleId.put(vehicleId, world.getTickIndex());
            }
        }
    }

    public MetaCell[] getMetaCellsUnits(Ownership ownership, Set<VehicleType> vehicleTypes) {
        List<MetaCell> metaCellsWithMyUnits = new LinkedList<>();
        for (int i = 0; i < metaCells.length; i++) {
            for (int j = 0; j < metaCells[0].length; j++) {
                MetaCell metaCell = metaCells[i][j];
                List<Vehicle> vehicles = streamVehicles(ownership, vehicleTypes).filter(veh -> {
                    boolean inside = veh.getX() >= metaCell.getX() &&
                            veh.getX() < metaCell.getX() + metaCell.getSize() &&
                            veh.getY() >= metaCell.getY() &&
                            veh.getY() < metaCell.getY() + metaCell.getSize();
                    return inside;
                }).collect(Collectors.toList());
                if (ownership.equals(MY)) {
                    metaCell.setMyVehicles(vehicles);
                }
                if (ownership.equals(ENEMY)) {
                    metaCell.setEnemyVehicles(vehicles);
                }
                if (!vehicles.isEmpty()) {
                    metaCellsWithMyUnits.add(metaCell);
                }
            }
        }
        return metaCellsWithMyUnits.stream().toArray(MetaCell[]::new);
    }

    private Stream<Vehicle> streamVehicles(Ownership ownership, Set<VehicleType> vehicleTypes) {
        Stream<Vehicle> stream = vehicleById.values().stream();

        switch (ownership) {
            case MY:
                stream = stream.filter(vehicle -> vehicle.getPlayerId() == PlayerExt.me.getId());
                break;
            case ENEMY:
                stream = stream.filter(vehicle -> vehicle.getPlayerId() != PlayerExt.me.getId());
                break;
            default:
        }

        if (vehicleTypes != null && !vehicleTypes.isEmpty()) {
            stream = stream.filter(vehicle -> vehicleTypes.contains(vehicle.getType()));
        }

        return stream;
    }

    private Stream<Vehicle> streamVehicles(Ownership ownership) {
        return streamVehicles(ownership, null);
    }

    private Stream<Vehicle> streamVehicles() {
        return streamVehicles(ANY);
    }

    public MetaGroup getMetaGroup(MetaCell metaCell, int vehicleCount) {
        return new MetaGroup(metaCell.getMyVehicles().subList(0, Math.min(vehicleCount, metaCell.getMyVehicles().size())));
    }

    public MetaCell getMetaCell(int i, int j) {
        return metaCells[i][j];
    }
}
