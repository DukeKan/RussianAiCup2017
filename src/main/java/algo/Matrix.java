package algo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by DukeKan on 12.11.2017.
 */
public class Matrix {
    public static void main(String[] args) {
        List<Integer> rowToRemove = Stream.of(2, 3).collect(Collectors.toList());
        List<Integer> colToRemove = Stream.of(1, 4).collect(Collectors.toList());

        int rows = 5;
        int cols = 5;
        int[][] matrix = new int[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = (int) (Math.random() * 10000);
            }
        }

        changeDimension(matrix, rowToRemove, colToRemove);
    }

    public static int[][] changeDimension(int[][] matrix, List<Integer> rowToRemove, List<Integer> colToRemove) {
        int[][] matrixCopy = matrix.clone();

        int[][] newMatrix = new int[matrix.length - rowToRemove.size()][matrix[0].length - colToRemove.size()];

        int tmpX = -1;

        for (int i = 0; i < matrix.length; i++) {
            tmpX++;
            if (rowToRemove.contains(i)) {
                tmpX--;
            }
            int tmpY = -1;
            for (int j = 0; j < matrix[0].length; j++) {
                tmpY++;
                if (colToRemove.contains(j)) {
                    tmpY--;
                }

                if (!rowToRemove.contains(i) && !colToRemove.contains(j)) {
                    newMatrix[tmpX][tmpY] = matrixCopy[i][j];
                }
            }
        }

        return newMatrix;
    }

    public static int[][] transpose (int[][] array) {
        if (array == null || array.length == 0)//empty or unset array, nothing do to here
            return array;

        int width = array.length;
        int height = array[0].length;

        int[][] array_new = new int[height][width];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                array_new[y][x] = array[x][y];
            }
        }
        return array_new;
    }

    public static void print(int[][] inp) {
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