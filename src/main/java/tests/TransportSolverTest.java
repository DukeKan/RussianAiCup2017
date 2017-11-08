package tests;

import algo.TransportSolver;

/**
 * Created by DukeKan on 08.11.2017.
 */
public class TransportSolverTest {
    public static void main(String[] args) {
        TransportSolver.print = true;
        closeTest();
        openTest();
    }

    private static void openTest() {
        // по горизонтали магазины (потребности)
        // по вертикали запасы (склады)
        int[][] c = new int[][]{
                { 2, 3, 4, 5},
                { 4, 5, 2, 3},
                { 6, 5, 2, 5}
        };

        int[] a = new int[]{4, 3, 5}; // запасы
        int[] b = new int[]{ 2, 3, 4, 3}; // потребности

        TransportSolver.solve(c, a, b);
    }

    private static void closeTest() {
        // по горизонтали магазины (потребности)
        // по вертикали запасы (склады)
        int[][] c = new int[][]{
                { 6, 6, 3, 5},
                { 5, 4, 4, 3},
                { 6, 5, 6, 4},
                { 8, 4, 2, 4}
        };

        int[] a = new int[]{80, 105, 125, 90}; // запасы
        int[] b = new int[]{110, 130, 160, 120}; // потребности

        TransportSolver.solve(c, a, b);
    }
}
