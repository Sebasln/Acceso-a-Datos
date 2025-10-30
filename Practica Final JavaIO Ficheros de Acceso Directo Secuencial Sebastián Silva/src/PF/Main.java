package PF;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class Main {
		
	
	    private static ArrayList<Empleado> listaEmpleados = new ArrayList<>();

	    public static void main(String[] args) {
	        cargarEmpleados(); 

	        System.out.println("Empleados cargados en el sistema:");
	        for (Empleado emp : listaEmpleados) {
	            System.out.println(emp);
	        }
	    }

	    public static void cargarEmpleados() {
	        try (FileInputStream fis = new FileInputStream("resources/empleado.dat");
	             ObjectInputStream ois = new ObjectInputStream(fis)) {
	            listaEmpleados = (ArrayList<Empleado>) ois.readObject();

	            System.out.println("Fichero de empleados cargado correctamente.");

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    
	}

}
