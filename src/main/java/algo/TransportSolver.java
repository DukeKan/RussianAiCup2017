package algo;

import java.util.Arrays;

/**
 * Created by DukeKan on 08.11.2017.
 */
public class TransportSolver {

    public static boolean print = false;

    /**
     * @param c стоимость пути от склада до магазина. По вертикали запасы (склады), по горизонтали потребность (магазины)
     * @param a запасы
     * @param b потребности
     * @return матрицу распределения частей запасов по магазинам
     */
    public static int[][] solve(int[][] c, int[] a, int[] b) {
        int asum = Arrays.stream(a).sum();
        int bsum = Arrays.stream(b).sum();
        boolean open = !(asum == bsum);
        if (open) {
            if (asum < bsum) {
                // потребность слишком большая
                // вводим фиктивный склад
                int ao = bsum - asum;
                a = copyIncreasing(a, ao);
                c = copyIncreasing(c, true);
            } else {
                // запасы больше потребности
                // вводим фиктивный магазин
                int bo = asum - bsum;
                b = copyIncreasing(b, bo);
                c = copyIncreasing(c, false);
            }
        }

        int[][] p = new int[c.length][c[0].length];

        int[] acopy = copy(a);
        int[] bcopy = copy(b);

        int secondStart = 0;
        for (int first = 0; first < p.length; first ++) {
            for (int second = secondStart; second < p[0].length; second++) {
                int min = Math.min(acopy[first], bcopy[second]);
                p[first][second] = min;
                acopy[first] = acopy[first] - min;
                bcopy[second] = bcopy[second] - min;
                // здесь дальше обнуляются столбцы или строки, но смысла нет
                boolean column = bcopy[second] == 0;
                if (column) {
                    secondStart ++;
                } else {
                    break;
                }
            }
        }

        checkCequalsB(p, b);
        //check(acopy, bcopy);
        if (print) {
            print(p);
        }
        return p;
    }

    private static void checkCequalsB(int[][] c, int[] b) {
        int sum = 0;
        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[0].length; j++) {
                sum += c[i][j];
            }
        }
        if (sum != Arrays.stream(b).sum()) {
            throw new IllegalStateException("Solution is not correct");
        }
    }

    private static void check(int[] acopy, int[] bcopy) {
        check(acopy);
        check(bcopy);
    }

    private static void check(int[] inp) {
        boolean right = Arrays.stream(inp).allMatch(i -> i == 0);
        if (!right) {
            throw new IllegalStateException("Incorrect algo");
        }
    }

    private static int[] copy(int[] a) {
        return Arrays.stream(a).toArray();
    }

    private static int[] copyIncreasing(int[] inp, int value) {
        int[] copy = new int[inp.length+1];
        System.arraycopy(inp, 0, copy, 0, inp.length);
        copy[inp.length] = value;
        return  copy;
    }

    private static int[][] copyIncreasing(int[][] inp, boolean addStorage) {
        int[][] copy;
        if (addStorage) {
            copy = new int[inp.length + 1][inp[0].length];
        } else {
            copy = new int[inp.length][inp[0].length + 1];
        }
        for (int i = 0; i < inp.length; i++) {
            for (int j = 0; j < inp[0].length; j++) {
                copy[i][j] = inp[i][j];
            }
        }
        return copy;
    }

    private static void print(int[][] inp) {
        System.out.println();
        System.out.println("Print");
        for (int i = 0; i < inp.length; i++) {
            System.out.println();
            for (int j = 0; j < inp[0].length; j++) {
                System.out.print("   " + inp[i][j]);
            }
        }
    }
}
