import algo.TransportSolver;
import datastruct.MetaCell;
import datastruct.MetaGroup;
import datastruct.PlayerExt;
import datastruct.WorldExt;
import model.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static datastruct.PlayerExt.Ownership.ENEMY;
import static datastruct.PlayerExt.Ownership.MY;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static model.VehicleType.*;
import static model.VehicleType.HELICOPTER;
import static model.VehicleType.TANK;

@SuppressWarnings({"UnsecureRandomNumberGeneration", "FieldCanBeLocal", "unused", "OverlyLongMethod"})
public final class MyStrategy implements Strategy {

    private WorldExt worldExt;
    private final Queue<Consumer<Move>> delayedMoves = new ArrayDeque<>();

    @Override
    public void move(Player me, World world, Game game, Move move) {
        PlayerExt.me = me;
        move.setLeft(0);
        move.setTop(0);
        move.setRight(world.getWidth());
        move.setBottom(world.getHeight());

        if (worldExt == null) {
            worldExt = new WorldExt(world);
            worldExt.separateByMetaCells(128);
        }
        worldExt.tick(world);

        if (me.getRemainingActionCooldownTicks() > 0) {
            return;
        }

        System.out.println("Delayed moves: " + delayedMoves.size());

        if (delayedMoves.isEmpty()) {
            for (VehicleType vehicleType : getVehicleTypes()) {

                PlayerExt.Ownership meOrEnemy = vehicleType.equals(ARRV) ? MY : ENEMY;

                MetaCell[] myCells = worldExt.getMetaCellsUnits(MY, of(vehicleType).collect(toSet()));
                MetaCell[] enemyCells = worldExt.getMetaCellsUnits(meOrEnemy, getPreferredTargetType(vehicleType));

                if (myCells.length == 0) {
                    return;
                }

                if (enemyCells.length == 0) {
                    enemyCells = worldExt.getMetaCellsUnits(meOrEnemy, getAnyTargetType(vehicleType));
                }

                if (enemyCells.length == 0) {
                    return;
                }

                int[][] distances = new int[myCells.length][enemyCells.length];
                for (int i = 0; i < myCells.length; i++) {
                    for (int j = 0; j < enemyCells.length; j++) {
                        int distance = myCells[i].distanceTo(enemyCells[j]);
                        distances[i][j] = distance;
                    }
                }

                int[] a = Arrays.stream(myCells).mapToInt(cell -> cell.getVehicles(MY).size()).toArray();
                int[] b = Arrays.stream(enemyCells).mapToInt(cell -> cell.getVehicles(meOrEnemy).size()).toArray();

                int[][] solve = TransportSolver.solve(distances, a, b);

                solve = optimizeSolve(myCells, enemyCells, solve);

                for (int i = 0; i < solve.length; i++) {
                    for (int j = 0; j < solve[0].length; j++) {
                        int solution = solve[i][j];
                        if (solution != 0) {
                            MetaCell myCell = myCells[i];
                            MetaCell enemyCell = enemyCells[j];

                            MetaGroup metaGroup = worldExt.getMetaGroup(myCell, solution);

                            if (!myCell.getVehicles(MY).isEmpty() && !enemyCell.getVehicles(meOrEnemy).isEmpty()) {
                                int xDist = enemyCell.getVehicleX(meOrEnemy) - myCell.getMyVehX();
                                int yDist = enemyCell.getVehicleY(meOrEnemy) - myCell.getMyVehY();

                                //System.out.println("Time: " + metaGroup.getTimeToPoint(xDist, yDist));
                                //if (metaGroup.getTimeToPoint(xDist, yDist) < 1000) {
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
            }
        } else {
            executeDelayedMove(move);
        }

    }

    private int[][] optimizeSolve(MetaCell[] myCells, MetaCell[] enemyCells, int[][] solve) {
        if (solve.length > myCells.length) {
            int newSizeI = myCells.length;
            int newSizeJ = solve[0].length;
            int diff = solve.length - newSizeI;
            int[][] newSolve = new int[newSizeI][newSizeJ];

            for (int i = solve.length - 1; i > solve.length - 1 - newSizeI; i--) {
                for (int j = solve[0].length - 1; j >= 0; j--) {
                    newSolve[i - diff][j] = solve[i][j];
                }
            }
            solve = newSolve;
        }

        if (solve[0].length > enemyCells.length) {
            int newSizeI = solve.length;
            int newSizeJ = enemyCells.length;
            int diff = solve[0].length - newSizeJ;
            int[][] newSolve = new int[newSizeI][newSizeJ];

            for (int i = solve.length - 1; i >= 0; i--) {
                for (int j = solve[0].length - 1; j > solve[0].length - 1 - newSizeJ; j--) {
                    newSolve[i][j - diff] = solve[i][j];
                }
            }
            solve = newSolve;
        }
        return solve;
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
        vehicleTypes.add(ARRV);
        vehicleTypes.add(FIGHTER);
        vehicleTypes.add(HELICOPTER);
        vehicleTypes.add(IFV);
        vehicleTypes.add(TANK);
        return vehicleTypes;
    }

    private boolean executeDelayedMove(Move move) {
        Consumer<Move> delayedMove = delayedMoves.poll();
        if (delayedMove == null) {
            return false;
        }

        delayedMove.accept(move);
        return true;
    }

}