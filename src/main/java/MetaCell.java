import model.TerrainType;
import model.Unit;
import model.Vehicle;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static model.TerrainType.FOREST;
import static model.TerrainType.PLAIN;
import static model.TerrainType.SWAMP;

/**
 * Created by DukeKan on 08.11.2017.
 */
public class MetaCell {
    private int x;
    private int y;
    private int i;
    private int j;
    private int size;
    private TerrainType[][] terrainTypes;
    private WorldExt worldExt;
    private TerrainType offtenTerrainType;
    private List<Vehicle> myVehicles;
    private List<Vehicle> enemyVehicles;

    public MetaCell(int x, int y, int i, int j, int size, TerrainType[][] terrainTypes, WorldExt worldExt) {
        this.x = x;
        this.y = y;
        this.i = i;
        this.j = j;
        this.size = size;
        this.terrainTypes = terrainTypes;
        this.worldExt = worldExt;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public TerrainType[][] getTerrainTypes() {
        return terrainTypes;
    }

    public List<Vehicle> getMyVehicles() {
        return myVehicles;
    }

    public void setMyVehicles(List<Vehicle> myVehicles) {
        this.myVehicles = myVehicles;
    }

    public List<Vehicle> getEnemyVehicles() {
        return enemyVehicles;
    }

    public List<Vehicle> getVehicles(PlayerExt.Ownership ownership) {
        if (ownership.equals(PlayerExt.Ownership.MY)) {
            return myVehicles;
        } else if (ownership.equals(PlayerExt.Ownership.ENEMY)){
            return enemyVehicles;
        } else {
            return null;
        }
    }

    public void setEnemyVehicles(List<Vehicle> enemyVehicles) {
        this.enemyVehicles = enemyVehicles;
    }

    public int distanceToWithEnemies(MetaCell other) {
        return (int) (pow((pow(this.x - other.x, 2) + pow(this.y - other.y, 2)), 0.5) *
                getEnemiesKoefficient(other.getEnemiesX(), other.getEnemiesY()));
    }

    public int distanceTo(MetaCell other) {
        return (int) (pow((pow(this.x - other.x, 2) + pow(this.y - other.y, 2)), 0.5));
    }

    private double getEnemiesKoefficient(int enemiesX, int enemiesY) {

        List<Vehicle> enemiesVehicles = worldExt.streamVehicles(PlayerExt.Ownership.ENEMY).filter(veh -> {
            boolean inside = veh.getX() >= this.getX() &&
                    veh.getX() < enemiesX &&
                    veh.getY() >= this.getY() &&
                    veh.getY() < enemiesY;
            return inside;
        }).collect(Collectors.toList());

        return  enemiesVehicles.size() * 0.1 ;
    }

    public int getVehicleX(PlayerExt.Ownership ownership) {
        if (ownership.equals(PlayerExt.Ownership.MY)) {
            return getMyVehX();
        }    else {
            return getEnemiesX();
        }
    }

    public int getVehicleY(PlayerExt.Ownership ownership) {
        if (ownership.equals(PlayerExt.Ownership.MY)) {
            return getMyVehY();
        }    else {
            return getEnemiesY();
        }
    }

    public int getEnemiesX(){
        OptionalDouble average = enemyVehicles.stream().mapToDouble(Unit::getX).average();
        if (average.isPresent()) {
            return (int) average.getAsDouble();
        } else {
            return getX();
        }
    }

    public int getEnemiesY(){
        OptionalDouble average = enemyVehicles.stream().mapToDouble(Unit::getY).average();
        if (average.isPresent()) {
            return (int) average.getAsDouble();
        } else {
            return getY();
        }
    }

    public int getMyVehX(){
        OptionalDouble average = myVehicles.stream().mapToDouble(Unit::getX).average();
        if (average.isPresent()) {
            return (int) average.getAsDouble();
        } else {
            return getX();
        }
    }

    public int getMyVehY(){
        OptionalDouble average = myVehicles.stream().mapToDouble(Unit::getY).average();
        if (average.isPresent()) {
            return (int) average.getAsDouble();
        } else {
            return getY();
        }
    }
}
