package datastruct;

import model.Facility;
import model.TerrainType;
import model.WeatherType;
import model.World;

/**
 * Created by DukeKan on 08.11.2017.
 */
public class WorldExt {
    private World world;

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

    public MetaCell[][] separateByMetaCells(int size){
        //todo обработать ситуацию, когда не делится нацело
        int width = (int)getWidth() / size;
        int height = (int) getHeight() / size;
        int i = 0;
        int j = 0;
        MetaCell[][] metaCells = new MetaCell[height + 1][width + 1];
        for (int x = 0; x < getHeight(); x = size + i * size, i++) {
            for (int y = 0; y < getWidth(); y = size + j * size, j++) {
                MetaCell metaCell = new MetaCell(x, y, size, size);
                metaCells[i][j] = metaCell;
            }
            j = 0;
        }
        return  metaCells;
    }
}
