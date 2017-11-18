import javafx.util.Pair;
import model.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static java.lang.Math.sin;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static model.VehicleType.*;

@SuppressWarnings({"UnsecureRandomNumberGeneration", "FieldCanBeLocal", "unused", "OverlyLongMethod"})
public final class MyStrategy implements Strategy {

    private WorldExt worldExt;
    private final Queue<Consumer<Move>> delayedMoves = new ArrayDeque<>();
    private Random random;
    private PlayerExt myPlayerExt;

    private int cellSize = 512;
    private int smallCellSize = 256;

    private boolean bombAssigned = false;
    private boolean enemyBombAssigned = false;

    private boolean scaled = false;

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
                enemyBombAssigned = true;

                // драпаем
                delayedMoves.add(delayedMove -> {
                    delayedMove.setAction(ActionType.CLEAR_AND_SELECT);
                    delayedMove.setLeft(nextNuclearStrikeX - 40);
                    delayedMove.setTop(nextNuclearStrikeY - 40);
                    delayedMove.setRight(nextNuclearStrikeX + 40);
                    delayedMove.setBottom(nextNuclearStrikeY + 40);
                });


                // надо доработать
                delayedMoves.add(delayedMove -> {
                    delayedMove.setAction(ActionType.MOVE);
                    delayedMove.setX(512);
                    delayedMove.setY(0);
                });
            }
        } else {
            enemyBombAssigned = false;
        }

        NuclearInfo nuclearInfo = worldExt.getNuclearBombCenter(70);

        if (myPlayerExt.nuclearBombAvailable() && !bombAssigned && !enemyBombAssigned && nuclearInfo != null) {
            delayedMoves.clear();

            delayedMoves.add(delayedMove -> delayedMove.setAction(ActionType.NONE));

            myPlayerExt.setNuclearVehicleDeadListener(() -> {
                // здесь нужно найти точку самого плотного скопления противника
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

                if (enemyCells.length == 0 && !scaled) { // разведка
                    for (MetaCell myCell : myCells) {
                        scale(world, myCell, 10);
                    }
                    scaled = true;
                    continue;
                }

                if (myCells.length == 0) {
                    continue;
                }

                int[][] distances = new int[myCells.length][enemyCells.length];
                for (int i = 0; i < myCells.length; i++) {
                    for (int j = 0; j < enemyCells.length; j++) {
                        int distance = myCells[i].distanceToWithEnemies(enemyCells[j]);
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

                            double myVehicleCount = myCell.getMyVehicles().size();
                            double enemyVehicleCount = enemyCell.getEnemyVehicles().size();

                            MetaGroup myMetaGroup = worldExt.getMetaGroup(PlayerExt.Ownership.MY, myCell, solution);
                            MetaGroup enemyMetaGroup = worldExt.getMetaGroup(PlayerExt.Ownership.ENEMY, enemyCell, solution);

                            if (!myCell.getVehicles(PlayerExt.Ownership.MY).isEmpty() && !enemyCell.getVehicles(PlayerExt.Ownership.ENEMY).isEmpty()) {
                                int timeToPoint = myMetaGroup.getTimeToPoint(cellDist);

                                // на слишком далёкие расстояния нет толку предсказывать
                                // форсируем события
                                if (timeToPoint > 200 && myMetaGroup.isMoving()) { // подумать над вариантом пропуска хода, если и так движемся в нужную точку.
                                   continue; // самая большая частота команд должна быть возле противника
                                }

                                Pair<Integer, Integer> positionInTime = enemyMetaGroup.getPositionInTime(timeToPoint);
                                myMetaGroup.setTargetPosition(positionInTime);

                                int bestDegree = 0;
                                int bestPointsEnemiesCount = 1000;
                                double bestX = 400;
                                double bestY = 400;
                                for (int degree = 0; degree < 360; degree += 20) {
                                    int x0 = positionInTime.getKey();
                                    int y0 = positionInTime.getValue();
                                    double r = myMetaGroup.getVehicles().stream().mapToDouble(VehicleExt::getAttackRange).average().getAsDouble() * 1.1;
                                    double alpha = ((degree * PI) / 180); // угол относительно оси x по часовой стрелке
                                    double x1 = x0 + r * cos(alpha);
                                    double y1 = y0 + r * sin(alpha);

                                    int pointEnemiesCount = worldExt.streamVehicles(PlayerExt.Ownership.ENEMY).filter(veh -> {
                                        return veh.getX() >= x1 - 100 &&
                                                veh.getX() < x1 + 100 &&
                                                veh.getY() >= y1 - 100 &&
                                                veh.getY() < y1 + 100;
                                    }).collect(Collectors.toList()).size();

                                    if (pointEnemiesCount < bestPointsEnemiesCount && x1 > 60 && y1 > 60 && x1 < 960 && y1 < 960) {
                                        bestX = x1;
                                        bestY = y1;
                                    }
                                }

                                double xDist = bestX - myCell.getMyVehX();
                                double yDist = bestY - myCell.getMyVehY();

                                delayedMoves.add(delayedMove -> {
                                    delayedMove.setAction(ActionType.CLEAR_AND_SELECT);
                                    delayedMove.setLeft(myMetaGroup.getVehicleX() - 50);
                                    delayedMove.setTop(myMetaGroup.getVehicleY() - 50);
                                    delayedMove.setRight(myMetaGroup.getVehicleX() + 50);
                                    delayedMove.setBottom(myMetaGroup.getVehicleY() + 50);
                                    delayedMove.setVehicleType(vehicleType);
                                });

                                delayedMoves.add(delayedMove -> {
                                    delayedMove.setAction(ActionType.SCALE);
                                    delayedMove.setX(myMetaGroup.getVehicleX());
                                    delayedMove.setY(myMetaGroup.getVehicleY());
                                    delayedMove.setFactor(0.5);
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
                if (world.getTickIndex() > 2000 && delayedMoves.size() < 3) {
                    for (MetaCell myCell : myCells) {
                        scale(world, myCell, 0.8);
                    }
                }
            }
        } else {
            executeDelayedMove(move);
        }
    }

    private void scale(World world, MetaCell myCell, double factor) {
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
                delayedMove.setAction(ActionType.SCALE);
                delayedMove.setX(x);
                delayedMove.setY(y);
                delayedMove.setFactor(factor);
            });
        }

        System.out.println("SCALING");
    }

    private boolean waitTooLong(World world, MetaCell myCell) {
        return myCell.getMyVehicles().stream().allMatch(
                vehicle -> world.getTickIndex() - WorldExt.updateTickByVehicleId.get(vehicle.getId()) > 300
        );
    }


    private boolean executeDelayedMove(Move move) {
        System.out.println("Delayed moves size: " + delayedMoves.size());
        Consumer<Move> delayedMove = delayedMoves.poll();
        if (delayedMove == null) {
            return false;
        }

        delayedMove.accept(move);
        return true;
    }
}