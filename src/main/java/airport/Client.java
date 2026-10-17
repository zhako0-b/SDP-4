package airport;
import airport.facade.*;
import java.nio.file.*;
import java.util.Scanner;
public class Client {
    public static void main(String[] args) {
        Path root = Path.of(args.length == 0 ? "data" : args[0]);
        var departure = new DepartureFacade(root); var admin = new AdminFacade(root);
        var input = new Scanner(System.in);
        while (true) {
            System.out.println("\n1 Depart  2 Inspect  3 Resume  4 Compensate  0 Exit");
            String choice = input.nextLine();
            if (choice.equals("0")) return;
            try {
                switch (choice) {
                    case "1" -> {
                        System.out.print("Journey ID: "); String id = input.nextLine();
                        System.out.print("Passenger: "); String name = input.nextLine();
                        System.out.print("Baggage kg (0-23): "); int kg = Integer.parseInt(input.nextLine());
                        System.out.print("Prohibited baggage? y/n: "); boolean prohibited = input.nextLine().equalsIgnoreCase("y");
                        System.out.print("Valid passport? y/n: "); boolean passport = input.nextLine().equalsIgnoreCase("y");
                        System.out.print("Customs declared? y/n: "); boolean customs = input.nextLine().equalsIgnoreCase("y");
                        System.out.print("Steps before pause (1=A, 2=A+B, 3=all): "); int steps = Integer.parseInt(input.nextLine());
                        departure.start(new Journey(id, name, kg, prohibited, passport, customs), steps);
                        System.out.println(admin.inspect(id));
                    }
                    case "2" -> { for (String id : admin.ids()) System.out.println(admin.inspect(id)); }
                    case "3", "4" -> {
                        System.out.print("Journey ID: "); String id = input.nextLine();
                        if (choice.equals("3")) departure.resume(id); else admin.compensate(id);
                        System.out.println(admin.inspect(id));
                    }
                    default -> System.out.println("Choose 0-4");
                }
            } catch (Exception e) { System.out.println("Error: " + e.getMessage() + ". Use Inspect to see persisted state."); }
        }
    }
}
