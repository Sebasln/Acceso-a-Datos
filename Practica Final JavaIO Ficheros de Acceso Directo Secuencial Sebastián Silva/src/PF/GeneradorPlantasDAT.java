package PF;

import java.io.RandomAccessFile;

public class GeneradorPlantasDAT {

	public static void main(String[] args) {

		try (RandomAccessFile raf = new RandomAccessFile("resources/plantas.dat", "rw")) {

			for (int i = 0; i < 20; i++) {
				int codigo = i + 1; 
				float precio = 5.0f + (i * 2.5f);
				int stock = 50 + (i * 5); 

				raf.writeInt(codigo);
				raf.writeFloat(precio);
				raf.writeInt(stock);

				System.out.println("Escribiendo: " + codigo + ", " + precio + ", " + stock);
			}

			System.out.println("Fichero 'plantas.dat' creado con éxito con 20 registros.");

		} catch (Exception e) {
			System.err.println("Error al generar 'plantas.dat': " + e.getMessage());
		}
	}
}