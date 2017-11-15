import javafx.util.Pair;
import model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static model.VehicleType.*;

/**
 * Created by DukeKan on 08.11.2017.
 */
public class WorldExt {
    private World world;
    private MetaCell[][] metaCells;
    public final Map<Long, Vehicle> vehicleById = new HashMap<>();
    public static final Map<Long, Integer> updateTickByVehicleId = new HashMap<>();
    public static final Map<Long, Pair<Double, Double>> positions = new HashMap<>();

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

    public Vehicle getVehicleById(Long id) {
        return vehicleById.get(id);
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
                MetaCell metaCell = new MetaCell(x, y, i, j, size, null, this);
                metaCells[i][j] = metaCell;
            }
            j = 0;
        }
    }

    public void tick(World world) {
        for (Vehicle vehicle : world.getNewVehicles()) {
            vehicleById.put(vehicle.getId(), vehicle);
            updateTickByVehicleId.put(vehicle.getId(), world.getTickIndex());
            positions.put(vehicle.getId(), new Pair<Double, Double>(vehicle.getX(), vehicle.getY()));
        }

        for (VehicleUpdate vehicleUpdate : world.getVehicleUpdates()) {
            long vehicleId = vehicleUpdate.getId();

            if (vehicleUpdate.getDurability() == 0) {
                vehicleById.remove(vehicleId);
                updateTickByVehicleId.remove(vehicleId);
                positions.remove(vehicleId);
            } else {
                positions.put(vehicleId, new Pair<>(vehicleById.get(vehicleId).getX(), vehicleById.get(vehicleId).getY()));
                Vehicle newVeh = new Vehicle(vehicleById.get(vehicleId), vehicleUpdate);
                vehicleById.put(vehicleId, newVeh);
                updateTickByVehicleId.put(vehicleId, world.getTickIndex());
            }
        }
    }

    public MetaCell[] getMetaCellsUnits(PlayerExt.Ownership ownership, boolean putToEnemies, Set<VehicleType> vehicleTypes) {
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
                if (ownership.equals(PlayerExt.Ownership.MY)) {
                    if (putToEnemies) {
                        metaCell.setEnemyVehicles(vehicles);
                    } else {
                        metaCell.setMyVehicles(vehicles);
                    }
                }
                if (ownership.equals(PlayerExt.Ownership.ENEMY)) {
                    metaCell.setEnemyVehicles(vehicles);
                }
                if (!vehicles.isEmpty()) {
                    metaCellsWithMyUnits.add(metaCell);
                }
            }
        }
        return metaCellsWithMyUnits.stream().toArray(MetaCell[]::new);
    }

    public Stream<Vehicle> streamVehicles(PlayerExt.Ownership ownership, Collection<VehicleType> vehicleTypes) {
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

    public Stream<Vehicle> streamVehicles(PlayerExt.Ownership ownership) {
        return streamVehicles(ownership, null);
    }

    private Stream<Vehicle> streamVehicles() {
        return streamVehicles(PlayerExt.Ownership.ANY);
    }

    public MetaGroup getMetaGroup(PlayerExt.Ownership ownership, MetaCell metaCell, int vehicleCount) {
        List<Vehicle> vehiclesByOwnership = metaCell.getVehicles(ownership);
        List<Vehicle> vehicles = vehiclesByOwnership.subList(0, Math.min(vehicleCount, vehiclesByOwnership.size()));
        List<Pair<Double, Double>> vehiclePositions = new ArrayList<>(vehicles.size());
        vehicles.forEach(veh -> vehiclePositions.add(positions.get(veh.getId())));
        return new MetaGroup(vehicles, vehiclePositions);
    }

    public MetaCell getMetaCell(int i, int j) {
        return metaCells[i][j];
    }

    public NuclearInfo getNuclearBombCenter(int windowSize) {
        double centerX = 512;
        double centerY = 512;
        double enemiesVehiclesDerivMyVehicles = -1;
        Vehicle vehicle = null;

        for (int x = 0; x < world.getWidth(); x += windowSize) {
            for (int y = 0; y < world.getHeight(); y += windowSize) {
                // здесь нужно учитывать 3 параметра
                // 1 - количество противников в квадрате
                // 2 - количество своих в квадрате
                // 3 - скорость противника в квадрате
                int finalX = x;
                int finalY = y;

                // чтобы не бомбил по ремонтникам
                List<VehicleType> preferableVehicles = Stream.of(TANK, IFV, FIGHTER, HELICOPTER).collect(Collectors.toList());
                List<Vehicle> enemyVehicles = streamVehicles(PlayerExt.Ownership.ENEMY, preferableVehicles).filter(veh -> {
                    boolean inside = veh.getX() >= finalX &&
                            veh.getX() < finalX + windowSize &&
                            veh.getY() >= finalY &&
                            veh.getY() < finalY + windowSize;
                    return inside;
                }).collect(Collectors.toList());

                // чтобы не бомбил по ремонтникам
                List<VehicleType> notPreferableVehicles = Stream.of(ARRV).collect(Collectors.toList());
                List<Vehicle> notEnemyVehicles = streamVehicles(PlayerExt.Ownership.ENEMY, notPreferableVehicles).filter(veh -> {
                    boolean inside = veh.getX() >= finalX &&
                            veh.getX() < finalX + windowSize &&
                            veh.getY() >= finalY &&
                            veh.getY() < finalY + windowSize;
                    return inside;
                }).collect(Collectors.toList());

                int enemiesCount = enemyVehicles.size() - notEnemyVehicles.size();
                if (enemiesCount < 0) {
                    enemiesCount = 0;
                }

                List<Vehicle> myVehicles = streamVehicles(PlayerExt.Ownership.MY, preferableVehicles).filter(veh -> {
                    boolean inside = veh.getX() >= finalX &&
                            veh.getX() < finalX + windowSize &&
                            veh.getY() >= finalY &&
                            veh.getY() < finalY + windowSize;
                    return inside;
                }).collect(Collectors.toList());

                if (!enemyVehicles.isEmpty() && !myVehicles.isEmpty()) {
                    double probablyX = enemyVehicles.stream().mapToDouble(veh -> veh.getX()).average().getAsDouble();
                    double probablyY = enemyVehicles.stream().mapToDouble(veh -> veh.getY()).average().getAsDouble();
                    double eneVehDerivMyVeh = ((double) enemiesCount) / myVehicles.size();
                    eneVehDerivMyVeh = eneVehDerivMyVeh * eneVehDerivMyVeh; // чтобы больший вес имели большие структуры противника
                    if (eneVehDerivMyVeh > enemiesVehiclesDerivMyVehicles) {
                        centerX = probablyX;
                        centerY = probablyY;
                        vehicle = myVehicles.iterator().next();
                        enemiesVehiclesDerivMyVehicles = eneVehDerivMyVeh;
                    }
                }
            }
        }

        if (enemiesVehiclesDerivMyVehicles < 0) {
            return null;
        }
        NuclearInfo nuclearInfo = new NuclearInfo();
        nuclearInfo.setCoordinates(new Pair<>(centerX, centerY));
        nuclearInfo.setVehicle(vehicle);
        return nuclearInfo;
    }

    public World getWorld() {
        return world;
    }
}
