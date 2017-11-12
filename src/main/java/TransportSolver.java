import java.util.*;

/**
 * Created by DukeKan on 08.11.2017.
 */
public class TransportSolver {

    public static boolean print = false;

    /**
     * Решение не оптимальное
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
                a = Matrix.copyIncreasing(a, ao);
                c = Matrix.copyIncreasing(c, true);
            } else {
                // запасы больше потребности
                // вводим фиктивный магазин
                int bo = asum - bsum;
                b = Matrix.copyIncreasing(b, bo);
                c = Matrix.copyIncreasing(c, false);
            }
        }

        int[][] p = new int[c.length][c[0].length];

        int[] acopy = Matrix.copy(a);
        int[] bcopy = Matrix.copy(b);

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

        if (print) {
            Matrix.print(p);
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


    /**
     * Оптимизация с целью уменьшения малых отрядов
     */
     public static int[][] optimizeSolution(MetaCell[] myCells, MetaCell[] enemyCells, int[][] solve) {
        List<Integer> rowsToRemove = new ArrayList<>();
        List<Integer> colsToRemove = new ArrayList<>();

        if (solve.length > myCells.length) {
            int newSizeI = myCells.length;
            int diff = solve.length - newSizeI;

            Map<Integer, Integer> sumToRow = new TreeMap<>();

            for (int i = 0; i < solve.length; i++) {
                sumToRow.put(Arrays.stream(solve[i]).sum(), i);
            }

            Iterator<Integer> iterator = sumToRow.keySet().iterator();

            for (int k = 0; k < diff; k++) {
                rowsToRemove.add(sumToRow.get(iterator.next()));
            }
        }
        if (solve[0].length > enemyCells.length) {
            int newSizeJ = enemyCells.length;
            int diff = solve[0].length - newSizeJ;

            Map<Integer, Integer> sumToCol = new TreeMap<>();

            int[][] transposed = Matrix.transpose(solve);

            for (int i = 0; i < transposed.length; i++) {
                sumToCol.put(Arrays.stream(transposed[i]).sum(), i);
            }

            Iterator<Integer> iterator = sumToCol.keySet().iterator();

            for (int k = 0; k < diff; k++) {
                colsToRemove.add(sumToCol.get(iterator.next()));
            }
        }

        return Matrix.changeDimension(solve, rowsToRemove, colsToRemove);
    }
}
