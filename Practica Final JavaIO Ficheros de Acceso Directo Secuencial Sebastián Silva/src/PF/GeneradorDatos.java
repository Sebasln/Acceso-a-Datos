package PF;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class GeneradorDatos {
    public static void main(String[] args) {
        ArrayList<Empleado> empleados = new ArrayList<>();

        empleados.add(new Empleado(1452, "Teresa", "asb123", "vendedor"));
        empleados.add(new Empleado(156, "Miguel Angel", "123qwe", "vendedor"));
        empleados.add(new Empleado(7532, "Natalia", "xs21qw4", "gestor"));

        try (FileOutputStream fos = new FileOutputStream("resources/empleado.dat");
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            
            oos.writeObject(empleados);
            System.out.println("-> 'empleado.dat' reseteado a 3 empleados.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}