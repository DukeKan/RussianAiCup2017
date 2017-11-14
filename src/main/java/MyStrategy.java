import javafx.util.Pair;
import model.*;

import java.util.*;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static model.VehicleType.ARRV;

@SuppressWarnings({"UnsecureRandomNumberGeneration", "FieldCanBeLocal", "unused", "OverlyLongMethod"})
public final class MyStrategy implements Strategy {

    private WorldExt worldExt;
    private final Queue<Consumer<Move>> delayedMoves = new ArrayDeque<>();
    private Random random;
    private PlayerExt myPlayerExt;

    private int cellSize = 256;
    private int smallCellSize = 64;

    private boolean bombAssigned = false;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        PlayerExt.me = me;
        PlayerExt.enemy = world.getOpponentPlayer();

        if (worldExt == null) {
            worldExt = new WorldExt(world);
            random = new Random(game.getRandomSeed());
            myPlayerExt = new PlayerExt(PlayerExt.me, worldExt);
        }

        worldExt.separateByMetaCells(cellSize);

        worldExt.tick(world);
        myPlayerExt.tick();

        if (worldExt.streamVehicles(PlayerExt.Ownership.MY).count() < 150 && cellSize != smallCellSize) {
            System.out.println("Small square!!!");
            cellSize = smallCellSize;
        }

        if (world.getTickIndex() < 2) {
            return;
        }

        if (me.getRemainingActionCooldownTicks() > 0) {
            return;
        }

        if (PlayerExt.enemy.getNextNuclearStrikeTickIndex() - game.getTickCount() < 30 &&
                PlayerExt.enemy.getNextNuclearStrikeVehicleId() < 0) {
            double nextNuclearStrikeX = PlayerExt.enemy.getNextNuclearStrikeX();
            double nextNuclearStrikeY = PlayerExt.enemy.getNextNuclearStrikeY();

            if (nextNuclearStrikeX > 0 && nextNuclearStrikeY > 0) {
                delayedMoves.clear();

                // драпаем
                delayedMoves.add(delayedMove -> {
                    delayedMove.setAction(ActionType.CLEAR_AND_SELECT);
                    delayedMove.setLeft(nextNuclearStrikeX - 40);
                    delayedMove.setTop(nextNuclearStrikeY - 40);
                    delayedMove.setRight(nextNuclearStrikeX + 40);
                    delayedMove.setBottom(nextNuclearStrikeY + 40);
                });

                delayedMoves.add(delayedMove -> {
                    delayedMove.setAction(ActionType.MOVE);
                    delayedMove.setX(512);
                    delayedMove.setY(0);
                });
            }
        }

        if (myPlayerExt.nuclearBombAvailable() && !bombAssigned) {
            delayedMoves.clear();

            delayedMoves.add(delayedMove -> delayedMove.setAction(ActionType.NONE));

            myPlayerExt.setNuclearVehicleDeadListener(() -> {
                // здесь нужно найти точку самого плотного скопления противника
                NuclearInfo nuclearInfo = worldExt.getNuclearBombCenter(64);
                if (nuclearInfo != null) {
                    myPlayerExt.setNuclearCoordinates(nuclearInfo.getCoordinates());
                    myPlayerExt.setNuclearBombVehicle(nuclearInfo.getVehicle());
                }
            });

            if (myPlayerExt.hasNuclearVehicle()) {
                bombAssigned = true;
                delayedMoves.add(delayedMove -> {
                    if (myPlayerExt.getNuclearBombVehicle() != null) {
                        System.out.println("BOMB!!!");
                        delayedMove.setAction(ActionType.TACTICAL_NUCLEAR_STRIKE);
                        delayedMove.setVehicleId(myPlayerExt.getNuclearBombVehicle().getId());
                        delayedMove.setX(myPlayerExt.getNuclearCoordinates().getKey());
                        delayedMove.setY(myPlayerExt.getNuclearCoordinates().getValue());
                        myPlayerExt.dropNuclearBomb();
                        bombAssigned = false;
                    } else {
                        delayedMove.setAction(ActionType.NONE);
                    }
                });
                delayedMoves.add(delayedMove -> {
                    if (myPlayerExt.getNuclearBombVehicle() != null) {
                        System.out.println("Freeze bomber!!!");
                        delayedMove.setAction(ActionType.CLEAR_AND_SELECT);
                        delayedMove.setVehicleType(myPlayerExt.getNuclearBombVehicle().getType());
                        delayedMove.setLeft(myPlayerExt.getNuclearBombVehicle().getX() - 10);
                        delayedMove.setRight(myPlayerExt.getNuclearBombVehicle().getX() + 10);
                        delayedMove.setTop(myPlayerExt.getNuclearBombVehicle().getY() - 10);
                        delayedMove.setBottom(myPlayerExt.getNuclearBombVehicle().getY() + 10);
                        delayedMove.setX(0);
                        delayedMove.setY(0);
                    } else {
                        delayedMove.setAction(ActionType.NONE);
                    }
                });
            }
        }

        List<VehicleType> vehicleTypes = VehicleExt.getVehicleTypes();
        if (delayedMoves.isEmpty()) {

            for (VehicleType vehicleType : vehicleTypes) {

                PlayerExt.Ownership meOrEnemy = vehicleType.equals(ARRV) ? PlayerExt.Ownership.MY : PlayerExt.Ownership.ENEMY;
                boolean putToEnemies = vehicleType.equals(ARRV);

                MetaCell[] myCells = worldExt.getMetaCellsUnits(PlayerExt.Ownership.MY, false, of(vehicleType).collect(toSet()));
                MetaCell[] enemyCells = worldExt.getMetaCellsUnits(meOrEnemy, putToEnemies, VehicleExt.getPreferredTargetType(vehicleType));

                if (enemyCells.length == 0) {
                    enemyCells = worldExt.getMetaCellsUnits(meOrEnemy, putToEnemies, VehicleExt.getAnyTargetType(vehicleType));
                }

                if (myCells.length == 0 || enemyCells.length == 0) { // вот здесь можно отправлять в край карты или на разведку
                    continue;
                }

                int[][] distances = new int[myCells.length][enemyCells.length];
                for (int i = 0; i < myCells.length; i++) {
                    for (int j = 0; j < enemyCells.length; j++) {
                        int distance = myCells[i].distanceTo(enemyCells[j]);
                        distances[i][j] = distance;
                    }
                }

                int[] a = Arrays.stream(myCells).mapToInt(cell -> cell.getVehicles(PlayerExt.Ownership.MY).size()).toArray();
                int[] b = Arrays.stream(enemyCells).mapToInt(cell -> cell.getVehicles(PlayerExt.Ownership.ENEMY).size()).toArray();

                int[][] solve = TransportSolver.solve(distances, a, b);

                solve = TransportSolver.optimizeSolution(myCells, enemyCells, solve);

                for (int i = 0; i < solve.length; i++) {
                    for (int j = 0; j < solve[0].length; j++) {
                        int solution = solve[i][j];
                        if (solution != 0) {
                            MetaCell myCell = myCells[i];
                            MetaCell enemyCell = enemyCells[j];

                            int cellDist = myCell.distanceTo(enemyCell);

                            if (cellDist < 3) {
                                continue;
                            }

                            if (waitTooLong(world, myCell)) {
                                rotate(world, myCell);
                                continue;
                            }

                            MetaGroup myMetaGroup = worldExt.getMetaGroup(PlayerExt.Ownership.MY, myCell, solution);
                            MetaGroup enemyMetaGroup = worldExt.getMetaGroup(PlayerExt.Ownership.ENEMY, enemyCell, solution);

                            if (!myCell.getVehicles(PlayerExt.Ownership.MY).isEmpty() && !enemyCell.getVehicles(PlayerExt.Ownership.ENEMY).isEmpty()) {
                                int timeToPoint = myMetaGroup.getTimeToPoint(cellDist);

                                Pair<Integer, Integer> positionInTime = enemyMetaGroup.getPositionInTime(timeToPoint / 2);

                                int xDist = positionInTime.getKey() - myCell.getMyVehX();
                                int yDist = positionInTime.getValue() - myCell.getMyVehY();

                                delayedMoves.add(delayedMove -> {
                                    delayedMove.setAction(ActionType.CLEAR_AND_SELECT);
                                    delayedMove.setLeft(myCell.getX());
                                    delayedMove.setTop(myCell.getY());
                                    delayedMove.setRight(myCell.getX() + myCell.getSize());
                                    delayedMove.setBottom(myCell.getY() + myCell.getSize());
                                    delayedMove.setVehicleType(vehicleType);
                                });

                                if (vehicleType.equals(ARRV)) {
                                    delayedMoves.add(delayedMove -> {
                                        delayedMove.setAction(ActionType.SCALE);
                                        double centerX = myMetaGroup.getVehicles().stream().mapToDouble(veh -> veh.getX()).average().getAsDouble();
                                        double centerY = myMetaGroup.getVehicles().stream().mapToDouble(veh -> veh.getY()).average().getAsDouble();
                                        delayedMove.setX( positionInTime.getKey() + (xDist) * 10);
                                        delayedMove.setY( positionInTime.getValue() + (yDist) * 10);
                                        delayedMove.setFactor(10);
                                        delayedMove.setVehicleType(vehicleType);
                                        System.out.println("Scaling ARRV");
                                    });
                                }

                                delayedMoves.add(delayedMove -> {
                                    delayedMove.setAction(ActionType.MOVE);
                                    delayedMove.setX(xDist);
                                    delayedMove.setY(yDist);
                                });
                            }
                        }
                    }
                }
            }
        } else {
            executeDelayedMove(move);
        }
    }

    private void rotate(World world, MetaCell myCell) {
        double x = myCell.getMyVehicles().stream().mapToDouble(Vehicle::getX).average().orElse(Double.NaN);
        double y = myCell.getMyVehicles().stream().mapToDouble(Vehicle::getY).average().orElse(Double.NaN);

        if (!Double.isNaN(x) && !Double.isNaN(y)) {
            delayedMoves.add(delayedMove -> {
                delayedMove.setAction(ActionType.CLEAR_AND_SELECT);
                delayedMove.setLeft(myCell.getX());
                delayedMove.setRight(myCell.getX() + myCell.getSize());
                delayedMove.setTop(myCell.getY());
                delayedMove.setBottom(myCell.getY() + myCell.getSize());
            });

            delayedMoves.add(delayedMove -> {
                delayedMove.setAction(ActionType.ROTATE);
                delayedMove.setX(x + (random.nextDouble() - 0.5) * 100);
                delayedMove.setY(y + (random.nextDouble() - 0.5) * 100);
                delayedMove.setAngle(random.nextBoolean() ? StrictMath.PI : -StrictMath.PI);
            });
        }

        System.out.println("Reformation");
    }

    private boolean waitTooLong(World world, MetaCell myCell) {
        return myCell.getMyVehicles().stream().allMatch(
                vehicle -> world.getTickIndex() - WorldExt.updateTickByVehicleId.get(vehicle.getId()) > 300
        );
    }


    private boolean executeDelayedMove(Move move) {
//        System.out.println("Delayed moves size: " + delayedMoves.size());
        Consumer<Move> delayedMove = delayedMoves.poll();
        if (delayedMove == null) {
            return false;
        }

        delayedMove.accept(move);
        return true;
    }
}