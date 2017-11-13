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

    @Override
    public void move(Player me, World world, Game game, Move move) {
        PlayerExt.me = me;

        if (worldExt == null) {
            worldExt = new WorldExt(world);
            random = new Random(game.getRandomSeed());
            myPlayerExt = new PlayerExt(PlayerExt.me, worldExt);
        }

        worldExt.separateByMetaCells(cellSize);

        worldExt.tick(world);
        myPlayerExt.tick();

        if (worldExt.streamVehicles(PlayerExt.Ownership.MY).count() < 100) {
            System.out.println("Small square!!!");
            cellSize = 32;
        }

        if (world.getTickIndex() < 2) {
            return;
        }

        if (me.getRemainingActionCooldownTicks() > 0) {
            return;
        }

        List<VehicleType> vehicleTypes = VehicleExt.getVehicleTypes();
        if (delayedMoves.isEmpty()) {
            if (myPlayerExt.nuclearBombAvailable()) {
                myPlayerExt.setNuclearVehicleDeadListener(() -> {
                    // здесь нужно найти точку самого плотного скопления противника
                    NuclearInfo nuclearInfo = worldExt.getNuclearBombCenter(64);
                    if (nuclearInfo != null) {
                        myPlayerExt.setNuclearCoordinates(nuclearInfo.getCoordinates());
                        myPlayerExt.setNuclearBombVehicle(nuclearInfo.getVehicle());
                    }
                });

                delayedMoves.add(delayedMove -> {
                    if (myPlayerExt.hasNuclearVehicle()) {
                        System.out.println("BOMB!!!");
                        delayedMove.setAction(ActionType.TACTICAL_NUCLEAR_STRIKE);
                        delayedMove.setVehicleId(myPlayerExt.getNuclearBombVehicle().getId());
                        delayedMove.setX(myPlayerExt.getNuclearCoordinates().getKey());
                        delayedMove.setY(myPlayerExt.getNuclearCoordinates().getValue());
                        myPlayerExt.dropNuclearBomb();
                    }
                });
            }
            for (VehicleType vehicleType : vehicleTypes) {

                PlayerExt.Ownership meOrEnemy = vehicleType.equals(ARRV) ? PlayerExt.Ownership.MY : PlayerExt.Ownership.ENEMY;
                boolean putToEnemies = vehicleType.equals(ARRV);

                MetaCell[] myCells = worldExt.getMetaCellsUnits(PlayerExt.Ownership.MY, false, of(vehicleType).collect(toSet()));
                MetaCell[] enemyCells = worldExt.getMetaCellsUnits(meOrEnemy, putToEnemies, VehicleExt.getPreferredTargetType(vehicleType));

                if (enemyCells.length == 0) {
                    enemyCells = worldExt.getMetaCellsUnits(meOrEnemy, putToEnemies, VehicleExt.getAnyTargetType(vehicleType));
                }

                if (myCells.length == 0 || enemyCells.length == 0) {
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
                delayedMove.setX(x + (random.nextDouble() - 0.5) * 20);
                delayedMove.setY(y + (random.nextDouble() - 0.5) * 20);
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
        //System.out.println("Delayed moves size: " + delayedMoves.size());
        Consumer<Move> delayedMove = delayedMoves.poll();
        if (delayedMove == null) {
            return false;
        }

        delayedMove.accept(move);
        return true;
    }
}