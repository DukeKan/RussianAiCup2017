package datastruct;

import datastruct.PlayerExt.Ownership;
import model.TerrainType;
import model.Unit;
import model.Vehicle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import static datastruct.PlayerExt.Ownership.ENEMY;
import static datastruct.PlayerExt.Ownership.MY;
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
    private TerrainType offtenTerrainType;
    private List<Vehicle> myVehicles;
    private List<Vehicle> enemyVehicles;


    public MetaCell(int x, int y, int i, int j, int size, TerrainType[][] terrainTypes) {
        this.x = x;
        this.y = y;
        this.i = i;
        this.j = j;
        this.size = size;
        this.terrainTypes = terrainTypes;
    }

    public TerrainType getOfftenTerrainType() {
        Map<TerrainType, Integer> typesCount = new HashMap<>();
        typesCount.put(FOREST, 0);
        typesCount.put(TerrainType.PLAIN, 0);
        typesCount.put(TerrainType.SWAMP, 0);
        for (int i = 0; i < terrainTypes.length; i++) {
            for (int j = 0; j < terrainTypes[0].length; j++) {
                TerrainType terrainType = terrainTypes[i][j];
                typesCount.put(terrainType, typesCount.get(terrainType) + 1);
            }
        }
        if (typesCount.get(FOREST) >= typesCount.get(PLAIN) && typesCount.get(FOREST) >= typesCount.get(SWAMP)) {
            return FOREST;
        }
        if (typesCount.get(PLAIN) >= typesCount.get(FOREST) && typesCount.get(PLAIN) >= typesCount.get(SWAMP)) {
            return PLAIN;
        }
        if (typesCount.get(SWAMP) >= typesCount.get(FOREST) && typesCount.get(SWAMP) >= typesCount.get(PLAIN)) {
            return SWAMP;
        }
        return null;
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

    public void setTerrainTypes(TerrainType[][] terrainTypes) {
        this.terrainTypes = terrainTypes;
    }

    public void setOfftenTerrainType(TerrainType offtenTerrainType) {
        this.offtenTerrainType = offtenTerrainType;
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

    public List<Vehicle> getVehicles(Ownership ownership) {
        if (ownership.equals(Ownership.MY)) {
            return myVehicles;
        } else if (ownership.equals(ENEMY)){
            return enemyVehicles;
        } else {
            return null;
        }
    }

    public void setEnemyVehicles(List<Vehicle> enemyVehicles) {
        this.enemyVehicles = enemyVehicles;
    }

    public int distanceTo(MetaCell other) {
        return (int) pow((pow(this.x - other.x, 2) + pow(this.y - other.y, 2)), 0.5);
    }

    public int getVehicleX(Ownership ownership) {
        if (ownership.equals(MY)) {
            return getMyVehX();
        }    else {
            return getEnemiesX();
        }
    }

    public int getVehicleY(Ownership ownership) {
        if (ownership.equals(MY)) {
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
