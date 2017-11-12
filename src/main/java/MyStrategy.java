import algo.Matrix;
import algo.TransportSolver;
import datastruct.MetaCell;
import datastruct.MetaGroup;
import datastruct.PlayerExt;
import datastruct.WorldExt;
import javafx.util.Pair;
import model.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static datastruct.PlayerExt.Ownership.ENEMY;
import static datastruct.PlayerExt.Ownership.MY;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static model.VehicleType.*;
import static model.VehicleType.HELICOPTER;
import static model.VehicleType.TANK;
import static sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl.ThreadStateMap.Byte0.runnable;

@SuppressWarnings({"UnsecureRandomNumberGeneration", "FieldCanBeLocal", "unused", "OverlyLongMethod"})
public final class MyStrategy implements Strategy {

    private WorldExt worldExt;
    private final Queue<Consumer<Move>> delayedMoves = new ArrayDeque<>();
    private Random random;

    private int cellSize = 256;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        PlayerExt.me = me;
        move.setLeft(0);
        move.setTop(0);
        move.setRight(world.getWidth());
        move.setBottom(world.getHeight());

        if (worldExt == null) {
            worldExt = new WorldExt(world);
            worldExt.separateByMetaCells(cellSize);
            random = new Random(game.getRandomSeed());
        }
        worldExt.tick(world);

        if (world.getTickIndex() < 2) {
            return;
        }

        if (me.getRemainingActionCooldownTicks() > 0) {
            return;
        }

        List<VehicleType> vehicleTypes = getVehicleTypes();
        if (delayedMoves.isEmpty()) {
            for (VehicleType vehicleType : vehicleTypes) {

                PlayerExt.Ownership meOrEnemy = vehicleType.equals(ARRV) ? MY : ENEMY;
                boolean putToEnemies = vehicleType.equals(ARRV);

                MetaCell[] myCells = worldExt.getMetaCellsUnits(MY, false, of(vehicleType).collect(toSet()));
                MetaCell[] enemyCells = worldExt.getMetaCellsUnits(meOrEnemy, putToEnemies, getPreferredTargetType(vehicleType));

                if (myCells.length == 0) {
                    continue;
                }

                if (enemyCells.length == 0) {
                    enemyCells = worldExt.getMetaCellsUnits(meOrEnemy, putToEnemies, getAnyTargetType(vehicleType));
                }

                if (enemyCells.length == 0) {
                    continue;
                }

                int[][] distances = new int[myCells.length][enemyCells.length];
                for (int i = 0; i < myCells.length; i++) {
                    for (int j = 0; j < enemyCells.length; j++) {
                        int distance = myCells[i].distanceTo(enemyCells[j]);
                        distances[i][j] = distance;
                    }
                }

                int[] a = Arrays.stream(myCells).mapToInt(cell -> cell.getVehicles(MY).size()).toArray();
                int[] b = Arrays.stream(enemyCells).mapToInt(cell -> cell.getVehicles(ENEMY).size()).toArray();

                int[][] solve = TransportSolver.solve(distances, a, b);

                solve = optimizeSolve(myCells, enemyCells, solve);

                for (int i = 0; i < solve.length; i++) {
                    for (int j = 0; j < solve[0].length; j++) {
                        int solution = solve[i][j];
                        if (solution != 0) {
                            MetaCell myCell = myCells[i];
                            MetaCell enemyCell = enemyCells[j];

                            int cellDist = myCell.distanceTo(enemyCell);

                            if (cellDist < 10) {
                                continue;
                            }

                            MetaGroup myMetaGroup = worldExt.getMetaGroup(MY, myCell, solution);
                            MetaGroup enemyMetaGroup = worldExt.getMetaGroup(ENEMY, enemyCell, solution);

                            if (!myCell.getVehicles(MY).isEmpty() && !enemyCell.getVehicles(ENEMY).isEmpty()) {
                                int timeToPoint = myMetaGroup.getTimeToPoint(cellDist);

                                //System.out.println(timeToPoint);

                                Pair<Integer, Integer> positionInTime = enemyMetaGroup.getPositionInTime(timeToPoint / 2);

                                int xDist = positionInTime.getKey() - myCell.getMyVehX();
                                int yDist = positionInTime.getValue() - myCell.getMyVehY();

                                //if (myCell.distanceTo(enemyCell) < 1000) {
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
                                //}
                            }
                        }
                    }
                }

                if (delayedMoves.isEmpty()) {
                    //throw new IllegalStateException("No delayed moves");
                }
            }
        } else {
            executeDelayedMove(move);
        }

    }

    private int[][] optimizeSolve(MetaCell[] myCells, MetaCell[] enemyCells, int[][] solve) {
        List<Integer> rowsToRemove = new ArrayList<>();
        List<Integer> colsToRemove = new ArrayList<>();

        if (solve.length > myCells.length) {
            int newSizeI = myCells.length;
            int newSizeJ = solve[0].length;
            int diff = solve.length - newSizeI;

            Map<Integer, Integer> sumToRow = new TreeMap<>();

            for (int i = 0; i < solve.length; i++) {
                sumToRow.put(Arrays.stream(solve[i]).sum(), i);
            }

            Iterator<Integer> iterator = sumToRow.keySet().iterator();

            for (int k = 0; k< diff; k++) {
                rowsToRemove.add(sumToRow.get(iterator.next()));
            }
        }
        if (solve[0].length > enemyCells.length) {
            int newSizeI = solve.length;
            int newSizeJ = enemyCells.length;
            int diff = solve[0].length - newSizeJ;

            Map<Integer, Integer> sumToCol = new TreeMap<>();

            int[][] transposed = Matrix.transpose(solve);

            for (int i = 0; i < transposed.length; i++) {
                sumToCol.put(Arrays.stream(transposed[i]).sum(), i);
            }

            Iterator<Integer> iterator = sumToCol.keySet().iterator();

            for (int k = 0; k< diff; k++) {
                colsToRemove.add(sumToCol.get(iterator.next()));
            }
        }

        return Matrix.changeDimension(solve, rowsToRemove, colsToRemove);
    }

    private static Set<VehicleType> getPreferredTargetType(VehicleType vehicleType) {
        switch (vehicleType) {
            case FIGHTER:
                return of(HELICOPTER).collect(toSet());
            case HELICOPTER:
                return of(TANK).collect(toSet());
            case IFV:
                return of(HELICOPTER).collect(toSet());
            case TANK:
                return of(IFV).collect(toSet());
            default:
                return of(TANK, IFV).collect(toSet());
        }
    }

    private Set<VehicleType> getAnyTargetType(VehicleType vehicleType) {
        switch (vehicleType) {
            case FIGHTER:
                return of(HELICOPTER, FIGHTER).collect(toSet());
            case HELICOPTER:
                return of(TANK, IFV, HELICOPTER, FIGHTER, ARRV).collect(toSet());
            case IFV:
                return of(TANK, IFV, HELICOPTER, FIGHTER, ARRV).collect(toSet());
            case TANK:
                return of(TANK, IFV, HELICOPTER, FIGHTER, ARRV).collect(toSet());
            default:
                return of(TANK, IFV, HELICOPTER, FIGHTER).collect(toSet());
        }
    }

    private List<VehicleType> getVehicleTypes() {
        List<VehicleType> vehicleTypes = new ArrayList<>(5);
        vehicleTypes.add(FIGHTER);
        vehicleTypes.add(HELICOPTER);
        vehicleTypes.add(IFV);
        vehicleTypes.add(TANK);
        vehicleTypes.add(ARRV);
        return vehicleTypes;
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