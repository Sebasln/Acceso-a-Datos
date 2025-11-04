package PF;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
		
	
	    private static ArrayList<Empleado> listaEmpleados = new ArrayList<>();

	    public static void main(String[] args) {
	    	Scanner sc = new Scanner(System.in);
	    	
	        cargarEmpleados(); 

	        	        
	        sc.close();
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

	    public static void revisarCarpeta(String nombreCarpeta) {
	        
	        File carpeta = new File(nombreCarpeta); 

	        if (!carpeta.exists()) {
	            System.out.println("La carpeta '" + nombreCarpeta + "' no existe, por lo que se va a crear ahora.");
	            
	            boolean creada = carpeta.mkdir();
	            
	            if (creada) {
	                System.out.println("Carpeta '" + nombreCarpeta + "' creada con éxito.");
	            } else {
	                System.err.println("No se pudo crear la carpeta '" + nombreCarpeta + "'.");
	            }
	        }
	    }
}
