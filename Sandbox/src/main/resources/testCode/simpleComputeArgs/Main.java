import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String line = scanner.nextLine().trim();
        String[] parts = line.split("\\s+");

        if (parts.length >= 2) {
            try {
                long a = Long.parseLong(parts[0]);
                long b = Long.parseLong(parts[1]);
                System.out.println(a + b);
            } catch (NumberFormatException e) {
                System.err.println("输入格式错误");
            }
        }
        scanner.close();
    }
}