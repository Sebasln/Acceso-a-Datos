package PF;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class Main {

	private static ArrayList<Empleado> listaEmpleados = new ArrayList<>();
	private static ArrayList<Planta> listaPlantas = new ArrayList<>();

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);

		cargarRecursos();
		login(sc);

		sc.close();
	}

	public static void menuVendedor(Scanner sc, Empleado eLogeado) {
		System.out.println("\nMenú de vendedor");

		int opcion;
		do {

			System.out.println("\n\t1 para mostrar el catálogo");
			System.out.println("\t2 para realizar una venta");
			System.out.println("\t3 para realizar una devolución");
			System.out.println("\t4 para buscar un ticket");
			System.out.println("\t5 para cerrar sesión");

			opcion = validarInt(sc, "Elige una opción: ");
			switch (opcion) {
			case 1:
				mostrarCatalogo();
				break;
			case 2:
				realizarVenta(sc, eLogeado);
				break;
			case 3:
				realizarDevolucion(sc);
				break;
			case 4:
				buscarTicket(sc);
				break;
			case 5:
				System.out.println("Saliendo...");
				break;
			default:
				System.err.println("Elige una de las opciones(1-3).");
			}
		} while (opcion != 5);
	}

	public static void mostrarCatalogo() {
		System.out.println("\nCatálogo de Plantas del Vivero");

		try (RandomAccessFile raf = new RandomAccessFile("resources/plantas.dat", "r")) {
			for (Planta planta : listaPlantas) {
				long posicion = (long) (planta.getCodigo() - 1) * 12;
				raf.seek(posicion);

				raf.readInt();
				float precio = raf.readFloat();
				int stock = raf.readInt();

				planta.setPrecio(precio);
				planta.setStock(stock);

				System.out.println("\t" + planta.toString());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void realizarVenta(Scanner sc, Empleado empleado) {
		System.out.println("\nMenú de venta");

		try {
			int codigo = validarInt(sc, "Introduce el código de la planta a vender: ");

			if (codigo < 1 || codigo > listaPlantas.size()) {
				System.err.println("El código de planta no existe.");
				return;
			}

			int cantidad = validarInt(sc, "Introduce la cantidad (ej: 10): ");

			if (cantidad <= 0) {
				System.err.println("La cantidad debe ser positiva.");
				return;
			}

			try (RandomAccessFile raf = new RandomAccessFile("resources/plantas.dat", "rw")) {

				long posicion = (long) (codigo - 1) * 12;
				raf.seek(posicion);

				raf.readInt();
				float precio = raf.readFloat();
				int stockActual = raf.readInt();

				if (cantidad > stockActual) {
					System.err.printf("Stock insuficiente. Sólo quedan %d unidades.\n", stockActual);
					return;
				}

				float total = precio * cantidad;
				String nombrePlanta = listaPlantas.get(codigo - 1).getNombre();

				System.out.println("\nResumen de compra:");

				System.out.printf("Planta: %s (Cód: %d)\n", nombrePlanta, codigo);
				System.out.printf("Cantidad: %d\n", cantidad);
				System.out.printf("Precio por planta: %.2f €\n", precio);
				System.out.printf("Total: %.2f €\n", total);

				System.out.print("¿Quiere realizar la compra? (s/n): ");

				String confirmacion = sc.next();

				if (!confirmacion.equals("s") && !confirmacion.equals("S")) {
					System.out.println("Venta cancelada.");
					return;
				}
				int nuevoStock = stockActual - cantidad;

				raf.seek(posicion + 8);
				raf.writeInt(nuevoStock);

				System.out.println("Venta realizada.");

				generarTicket(empleado, codigo, nombrePlanta, cantidad, precio, total);

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void realizarDevolucion(Scanner sc) {
		System.out.println("\nMenú de Devolución");

		int numTicket = validarInt(sc, "Introduce el número de ticket a devolver: ");

		String nombreFOriginal = "TICKETS/" + numTicket + ".txt";
		String nombreFDevuelto = "DEVOLUCIONES/" + numTicket + ".txt";

		File fTicketOriginal = new File(nombreFOriginal);
		File fTicketDevuelto = new File(nombreFDevuelto);

		if (!fTicketOriginal.exists()) {
			System.err.println("El ticket " + numTicket + ".txt no existe en la carpeta TICKETS/.");
			return;
		}
		if (fTicketDevuelto.exists()) {
			System.err.println("El ticket " + numTicket + ".txt ya fue procesado como una devolución.");
			return;
		}

		int codigoPlanta = -1;
		int cantidadDevolver = -1;
		ArrayList<String> contenidoTicket = new ArrayList<>();

		try {
			try (FileReader fr = new FileReader(fTicketOriginal); BufferedReader br = new BufferedReader(fr)) {

				String linea;
				while ((linea = br.readLine()) != null) {
					contenidoTicket.add(linea);

					if (linea.contains("(Cód:")) {
						int iniCodigo = linea.indexOf("(Cód:") + 5;
						int finCodigo = linea.indexOf(")", iniCodigo);
						codigoPlanta = Integer.parseInt(linea.substring(iniCodigo, finCodigo));

						int iniCant = linea.indexOf("\t", finCodigo) + 1;
						int finCant = linea.indexOf("\t", iniCant);
						cantidadDevolver = Integer.parseInt(linea.substring(iniCant, finCant).trim());
					}
				}
			}

			if (codigoPlanta == -1 || cantidadDevolver == -1) {
				System.err.println("El formato del ticket " + numTicket + ".txt es incorrecto.");
				return;
			}

			try (RandomAccessFile raf = new RandomAccessFile("resources/plantas.dat", "rw")) {
				long posicion = (long) (codigoPlanta - 1) * 12;
				raf.seek(posicion + 8);

				int stockActual = raf.readInt();
				int nuevoStock = stockActual + cantidadDevolver;

				raf.seek(posicion + 8);
				raf.writeInt(nuevoStock);
			}

			revisarCarpeta("DEVOLUCIONES");

			try (FileWriter fw = new FileWriter(fTicketDevuelto); PrintWriter pw = new PrintWriter(fw)) {

				for (String linea : contenidoTicket) {
					if (linea.startsWith("TOTAL A PAGAR: ")) {
						try {
							String totalStr = linea.substring(15).replace(" €", "").replace(",", ".");
							double totalNum = Double.parseDouble(totalStr);

							totalNum = -totalNum;

							pw.println("TOTAL DEVUELTO: " + String.format("%.2f €", totalNum));

						} catch (Exception e) {
							pw.println(linea);
						}
					} else {
						pw.println(linea);
					}
				}

				pw.println("---------------------------------");
				pw.println("--- TICKET DEVUELTO ---");
				pw.println("--- STOCK RESTAURADO ---");
			}

			fTicketOriginal.delete();

			System.out.println("Devolución completada");
			System.out.printf("Se han devuelto %d unidades de la planta con código %d a su stock.\n", cantidadDevolver,
					codigoPlanta);
			System.out.println("Ticket " + numTicket + ".txt movido a DEVOLUCIONES.");

		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}

	public static void buscarTicket(Scanner sc) {
		System.out.println("\nBúsqueda de Ticket");
		int numTicket = validarInt(sc, "Introduce el número de ticket a buscar: ");

		String nombreFichero = numTicket + ".txt";
		File ficheroTicket = new File("TICKETS/" + nombreFichero);
		File ficheroDevuelto = new File("DEVOLUCIONES/" + nombreFichero);

		File ficheroAImprimir = null;

		if (ficheroTicket.exists()) {
			ficheroAImprimir = ficheroTicket;
			System.out.println("Ticket encontrado en VENTAS ACTIVAS/.");
		} else if (ficheroDevuelto.exists()) {
			ficheroAImprimir = ficheroDevuelto;
			System.out.println("Ticket encontrado en DEVOLUCIONES/.");
		} else {
			System.err.println("Error: No se ha encontrado ningún ticket con el número " + numTicket);
			return;
		}

		System.out.println("\nContenido del ticket " + numTicket + ":\n");
		try (FileReader fr = new FileReader(ficheroAImprimir); BufferedReader br = new BufferedReader(fr)) {

			String linea;
			while ((linea = br.readLine()) != null) {
				System.out.println(linea);
			}

		} catch (IOException e) {
			System.err.println("Error al leer el fichero del ticket.");
		}
		System.out.println("\n\tFin del ticket.");
	}

	public static void menuGestor(Scanner sc, Empleado eLogeado) {
		System.out.println("\nMenú de Gestor");

		int opcion;
		do {
			System.out.println("\n\t1 para dar de alta una planta");
			System.out.println("\t2 para dar de baja una planta");
			System.out.println("\t3 para modificar stock o precio de planta");
			System.out.println("\t4 para dar de alta un empleado");
			System.out.println("\t5 para dar de baja un empleado");
			System.out.println("\t6 para rescatar a un empleado que fue dado de baja");
			System.out.println("\t7 para ver estadísticas de ventas");
			System.out.println("\t8 para cerrar sesión");

			opcion = validarInt(sc, "Elige una opción (1-7): ");

			switch (opcion) {
			case 1:
				darAltaPlanta(sc);
				break;
			case 2:
				darBajaPlanta(sc);
				break;
			case 3:
				modificarPlanta(sc);
				break;
			case 4:
				darAltaEmpleado(sc);
				break;
			case 5:
				darBajaEmpleado(sc);
				break;
			case 6:
				rescatarEmpleado(sc);
				break;
			case 7:
				verEstadisticas();
				break;
			case 8:
				System.out.println("Saliendo...");
				break;
			default:
				System.err.println("Elige una de las opciones(1-7).");
			}
		} while (opcion != 8);
	}

	public static void darAltaPlanta(Scanner sc) {
		System.out.println("\nDar de alta a una planta");
		sc.nextLine();

		System.out.print("Introduce el nombre: ");
		String nombre = sc.nextLine();
		System.out.print("Introduce la foto (ej: 'nueva.jpg'): ");
		String foto = sc.nextLine();
		System.out.print("Introduce la descripción: ");
		String descripcion = sc.nextLine();

		float precio = validarFloat(sc, "Introduce el precio (ej: 12,50): ");
		int stock = validarInt(sc, "Introduce el stock inicial: ");

		int nuevoCodigo = listaPlantas.size() + 1;

		Planta nuevaPlanta = new Planta(nuevoCodigo, nombre, foto, descripcion);

		nuevaPlanta.setPrecio(precio);
		nuevaPlanta.setStock(stock);

		listaPlantas.add(nuevaPlanta);

		System.out.println("Planta añadida a la lista.");

		escribirPlantaEnDat(nuevaPlanta);

		escribirPlantasXML();

		System.out.println("\nPlanta dada de alta en ambos ficheros.");
		System.out.println(nuevaPlanta.toString());
	}

	public static void darBajaPlanta(Scanner sc) {
		System.out.println("\nDar de baja a una planta");

		int codigo = validarInt(sc, "Introduce el código de la planta a dar de baja: ");
		if (codigo < 1 || codigo > listaPlantas.size()) {
			System.err.println("El código de planta no existe.");
			return;
		}

		try (RandomAccessFile raf = new RandomAccessFile("resources/plantas.dat", "rw")) {
			long posicion = (long) (codigo - 1) * 12;
			raf.seek(posicion);

			raf.readInt();
			float precioOriginal = raf.readFloat();
			int stockOriginal = raf.readInt();

			if (precioOriginal == 0.0f && stockOriginal == 0) {
				System.err.println("La planta " + codigo + " ya estaba dada de baja.");
				return;
			}

			raf.seek(posicion + 4);
			raf.writeFloat(0.0f);
			raf.writeInt(0);

			escribirBajaPlanta(codigo, precioOriginal);

			System.out.printf("La planta con código %d ha sido dada de alta.", codigo);
			System.out.println("Stock y precio puestos a 0 en plantas.dat.");
		} catch (IOException e) {
			System.err.println("Error de E/S al dar de baja la planta.");
		}
	}

	public static void modificarPlanta(Scanner sc) {
		System.out.println("\nModificar stock o precio de una planta");

		int codigo = validarInt(sc, "Introduce el código de la planta a modificar: ");
		if (codigo < 1 || codigo > listaPlantas.size()) {
			System.err.println("El código de planta no existe.");
			return;
		}

		try (RandomAccessFile raf = new RandomAccessFile("resources/plantas.dat", "rw")) {
			long posicion = (long) (codigo - 1) * 12;
			raf.seek(posicion);
			raf.readInt();
			float precioActual = raf.readFloat();
			raf.readInt();

			float nuevoPrecio;
			int nuevoStock;

			if (precioActual == 0.0f) {
				float precioRescatado = buscarPrecioEnBajas(codigo);

				if (precioRescatado == -1.0f) {
					System.err.println(
							"La planta está de baja, pero no se encontró su precio en 'plantasBaja.dat'. Por favor, introduzca un precio manualmente.");
					nuevoPrecio = validarFloat(sc, "Introduce el nuevo precio: ");
				} else {
					System.out.printf("Se restaurará a: %.2f € (precio original)\n", precioRescatado);
					nuevoPrecio = precioRescatado;
				}
				nuevoStock = validarInt(sc, "Introduce el nuevo stock: ");
			} else {
				System.out.println("Datos actuales: Precio=" + precioActual);
				nuevoPrecio = validarFloat(sc, "Introduce el nuevo precio: ");
				nuevoStock = validarInt(sc, "Introduce el nuevo stock: ");
			}

			raf.seek(posicion + 4);
			raf.writeFloat(nuevoPrecio);
			raf.writeInt(nuevoStock);

			System.out.println("Planta actualizada en plantas.dat");

			if (precioActual == 0.0f && nuevoPrecio > 0.0f) {
				eliminarPlantaDeBajas(codigo);
			}

		} catch (IOException e) {
			System.err.println("Error de E/S al modificar la planta.");
		}
	}

	public static void darAltaEmpleado(Scanner sc) {
		System.out.println("\nDar de alta a un empleado");

		int id;
		boolean idRepetido;
		do {
			idRepetido = false;
			id = validarInt(sc, "Introduce el ID del nuevo empleado (ej: 1, 42, 1001): ");

			for (Empleado emp : listaEmpleados) {
				if (emp.getIdentificacion() == id) {
					System.err.println("Error: ¡El ID " + id + " ya pertenece a " + emp.getNombre() + "!");
					idRepetido = true;
					break;
				}
			}

			if (!idRepetido) {
				for (Empleado empBaja : cargarEmpleadosDeBaja()) {
					if (empBaja.getIdentificacion() == id) {
						System.err.println("Error: ¡El ID " + id + " ya pertenece a un empleado de baja!");
						idRepetido = true;
						break;
					}
				}
			}

		} while (idRepetido);

		sc.nextLine();

		System.out.print("Introduce el nombre: ");
		String nombre = sc.nextLine();

		String pass = validarPassword(sc);

		String cargo = validarCargo(sc);

		Empleado nuevoEmp = new Empleado(id, nombre, pass, cargo);
		listaEmpleados.add(nuevoEmp);

		guardarEmpleados();

		System.out.printf("\nEl empleado %s con id %04d ha sido dado de alta.", nuevoEmp.getNombre(),
				nuevoEmp.getIdentificacion());
		

	}

	public static void darBajaEmpleado(Scanner sc) {
		System.out.println("\nDar de baja a un empleado");

		int idBaja = validarInt(sc, "Introduce el id del empleado a dar de baja: ");

		Empleado empleadoADespedir = null;
		for (Empleado emp : listaEmpleados) {
			if (emp.getIdentificacion() == idBaja) {
				empleadoADespedir = emp;
				break;
			}
		}

		if (empleadoADespedir == null) {
			System.err.println("No se ha encontrado ningún empleado con el id " + idBaja);
			return;
		}

		ArrayList<Empleado> listaDeBajas = cargarEmpleadosDeBaja();

		listaDeBajas.add(empleadoADespedir);

		guardarEmpleadosDeBaja(listaDeBajas);

		listaEmpleados.remove(empleadoADespedir);

		guardarEmpleados();

		System.out.printf("El empleado %s con id %04d ha sido dado de baja. ", empleadoADespedir.getNombre(),
				empleadoADespedir.getIdentificacion());
		System.out.println("Se ha movido a BAJA/empleadosBaja.dat");
	}

	public static void rescatarEmpleado(Scanner sc) {
		System.out.println("\nRescate de un empleado");

		ArrayList<Empleado> listaDeBajas = cargarEmpleadosDeBaja();

		if (listaDeBajas.isEmpty()) {
			System.out.println("No hay empleados dados de baja para rescatar.");
			return;
		}

		System.out.println("Empleados actualmente de baja:");
		for (Empleado emp : listaDeBajas) {
			System.out.printf("\tID: %04d - Nombre: %s\n", emp.getIdentificacion(), emp.getNombre());
		}

		int idRescatar = validarInt(sc, "Introduce el ID del empleado a rescatar: ");

		Empleado empleadoARescatar = null;
		for (Empleado emp : listaDeBajas) {
			if (emp.getIdentificacion() == idRescatar) {
				empleadoARescatar = emp;
				break;
			}
		}

		if (empleadoARescatar == null) {
			System.err.println("No se ha encontrado ningún empleado de baja con el ID " + idRescatar);
			return;
		}

		listaDeBajas.remove(empleadoARescatar);
		listaEmpleados.add(empleadoARescatar);

		guardarEmpleadosDeBaja(listaDeBajas);
		guardarEmpleados();

		System.out.printf("Se ha rescatado al empleado  %s con id %04d", empleadoARescatar.getNombre(),
				empleadoARescatar.getIdentificacion());
	}

	public static void verEstadisticas() {
		System.out.println("\nEstadísitcas de ventas");

		File carpetaTickets = new File("TICKETS");
		File[] listaTickets = carpetaTickets.listFiles();

		if (listaTickets == null || listaTickets.length == 0) {
			System.out.println("No hay tickets de venta para generar estadísticas.");
			return;
		}

		double totalRecaudado = 0.0;
		HashMap<Integer, Integer> ventasPorPlanta = new HashMap<>();

		for (File ticket : listaTickets) {
			if (ticket.getName().endsWith(".txt")) {
				try (FileReader fr = new FileReader(ticket); BufferedReader br = new BufferedReader(fr)) {

					String linea;
					while ((linea = br.readLine()) != null) {

						if (linea.startsWith("TOTAL A PAGAR: ")) {
							String totalStr = linea.substring(15).replace(" €", "").replace(",", ".");
							totalRecaudado += Double.parseDouble(totalStr);
						}

						if (linea.contains("(Cód:")) {

							int iniCodigo = linea.indexOf("(Cód:") + 5;
							int finCodigo = linea.indexOf(")", iniCodigo);
							int codigoPlanta = Integer.parseInt(linea.substring(iniCodigo, finCodigo));

							int iniCant = linea.indexOf("\t", finCodigo) + 1;
							int finCant = linea.indexOf("\t", iniCant);
							int cantidad = Integer.parseInt(linea.substring(iniCant, finCant).trim());

							ventasPorPlanta.put(codigoPlanta, ventasPorPlanta.getOrDefault(codigoPlanta, 0) + cantidad);
						}
					}
				} catch (Exception e) {
					System.err.println("Error al leer el ticket " + ticket.getName() + ".");
				}
			}
		}
		System.out.printf("Total Recaudado (sólo ventas activas): %.2f €\n", totalRecaudado);

		if (ventasPorPlanta.isEmpty()) {
			System.out.println("No se han vendido plantas.");
		} else {
			int maxVentas = 0;
			int codPlantaMasVendida = -1;

			for (Map.Entry<Integer, Integer> entry : ventasPorPlanta.entrySet()) {
				if (entry.getValue() > maxVentas) {
					maxVentas = entry.getValue();
					codPlantaMasVendida = entry.getKey();
				}
			}

			String nombrePlanta = "Desconocida";
			if (codPlantaMasVendida > 0 && codPlantaMasVendida <= listaPlantas.size()) {
				nombrePlanta = listaPlantas.get(codPlantaMasVendida - 1).getNombre();
			}

			System.out.printf("La planta %s con código %d ha sido la más vendida.", nombrePlanta, codPlantaMasVendida);
			System.out.println("Unidades vendidas: " + maxVentas);
		}
	}

	public static void login(Scanner sc) {
		System.out.println();
		System.out.println("Bienvenido al vivero.");
		int nIntentos = 0;

		do {

			int id = validarInt(sc, "\tIntroduce tu ID: ");
			System.out.print("\tIntroduce tu contraseña: ");
			String pass = sc.next();

			Empleado empleadoLogueado = validarUsuario(id, pass);

			if (empleadoLogueado != null) {
				System.out.printf("El empleado %s ha accedido al sistema.", empleadoLogueado.getNombre());
				if (empleadoLogueado.getCargo().equalsIgnoreCase("vendedor")) {
					menuVendedor(sc, empleadoLogueado);
					break;
				} else if (empleadoLogueado.getCargo().equalsIgnoreCase("gestor")) {
					menuGestor(sc, empleadoLogueado);
					break;
				}
			} else {
				System.err.println("ID o contraseña equivocado.");
				nIntentos++;
			}
		} while (nIntentos < 3);

		if (nIntentos >= 3) {
			System.out.println("Límite de intentos excedido.");
		}

	}

	@SuppressWarnings("unchecked")
	public static ArrayList<Empleado> cargarEmpleadosDeBaja() {
		revisarCarpeta("BAJA");
		String ficheroBajas = "BAJA/empleadosBaja.dat";
		ArrayList<Empleado> listaDeBajas = new ArrayList<>();
		File f = new File(ficheroBajas);

		if (f.exists()) {
			try (FileInputStream fis = new FileInputStream(ficheroBajas);
					ObjectInputStream ois = new ObjectInputStream(fis)) {

				listaDeBajas = (ArrayList<Empleado>) ois.readObject();

			} catch (Exception e) {
				System.err.println("No se pudo leer 'empleadosBaja.dat'.");
			}
		}
		return listaDeBajas;
	}

	public static void guardarEmpleadosDeBaja(ArrayList<Empleado> listaDeBajas) {
		revisarCarpeta("BAJA");
		String ficheroBajas = "BAJA/empleadosBaja.dat";

		try (FileOutputStream fos = new FileOutputStream(ficheroBajas);
				ObjectOutputStream oos = new ObjectOutputStream(fos)) {

			oos.writeObject(listaDeBajas);

		} catch (IOException e) {
			System.err.println("Error al guardar en 'empleadosBaja.dat'.");
		}
	}

	public static void generarTicket(Empleado empleado, int codProd, String nomProd, int cantidad, float precioUd,
			float total) {

		int ticketNum = obtenerSiguienteNumeroTicket();

		revisarCarpeta("TICKETS");
		String nombreFichero = "TICKETS/" + ticketNum + ".txt";

		try (FileWriter fw = new FileWriter(nombreFichero); PrintWriter pw = new PrintWriter(fw)) {

			pw.println("Número Ticket: " + ticketNum);
			pw.println();
			pw.printf("Empleado que ha atendido: %04d\n", empleado.getIdentificacion());
			pw.println("Nombre del empleado: " + empleado.getNombre());
			pw.println("Fecha de venta: " + LocalDate.now());
			pw.println();
			pw.println("Producto\tCant.\tPrecio Ud.\tTotal");
			pw.printf("%s (Cód:%d)\t%d\t%.2f€\t\t%.2f€\n", nomProd, codProd, cantidad, precioUd, total);
			pw.println();
			pw.println("TOTAL A PAGAR: " + String.format("%.2f €", total));

			System.out.println("Ticket " + ticketNum + ".txt generado con éxito.");

		} catch (IOException e) {
			System.err.println("Error al generar el ticket.");
		}
	}

	public static int obtenerSiguienteNumeroTicket() {
		revisarCarpeta("TICKETS");
		revisarCarpeta("DEVOLUCIONES");

		int maxTickets = buscarMaxNumEnCarpeta("TICKETS");
		int maxDevoluciones = buscarMaxNumEnCarpeta("DEVOLUCIONES");

		int maxGlobal = Math.max(maxTickets, maxDevoluciones);

		return maxGlobal + 1;
	}

	private static int buscarMaxNumEnCarpeta(String nombreCarpeta) {
		File carpeta = new File(nombreCarpeta);

		if (!carpeta.exists()) {
			return 0;
		}

		File[] ficheros = carpeta.listFiles();
		int maxNum = 0;

		if (ficheros == null) {
			return 0;
		}

		for (File f : ficheros) {
			String nombre = f.getName();
			if (!nombre.endsWith(".txt")) {
				continue;
			}
			try {
				String numStr = nombre.substring(0, nombre.lastIndexOf('.'));
				int num = Integer.parseInt(numStr);
				if (num > maxNum) {
					maxNum = num;
				}
			} catch (Exception e) {
				e.printStackTrace();

			}
		}

		return maxNum;
	}

	public static void escribirPlantasXML() {

		try (FileWriter fw = new FileWriter("resources/plantas.xml"); PrintWriter pw = new PrintWriter(fw)) {

			pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

			pw.print("<plantas>");

			for (Planta p : listaPlantas) {
				pw.print("<planta>");
				pw.print("<codigo>" + p.getCodigo() + "</codigo>");
				pw.print("<nombre>" + p.getNombre() + "</nombre>");
				pw.print("<foto>" + p.getFoto() + "</foto>");
				pw.print("<descripcion>" + p.getDescripcion() + "</descripcion>");

				pw.print("</planta>");
			}

			pw.print("</plantas>");

		} catch (IOException e) {
			System.err.println("Error al sobrescribir 'plantas.xml'.");
		}
	}

	public static void eliminarPlantaDeBajas(int codigoARescatar) {
		File ficheroBajas = new File("resources/plantasBaja.dat");
		if (!ficheroBajas.exists())
			return;

		ArrayList<Integer> codigosGuardar = new ArrayList<>();
		ArrayList<Float> preciosGuardar = new ArrayList<>();

		try {
			try (RandomAccessFile rafRead = new RandomAccessFile(ficheroBajas, "r")) {
				while (rafRead.getFilePointer() < rafRead.length()) {
					int cod = rafRead.readInt();
					float precio = rafRead.readFloat();

					if (cod != codigoARescatar) {
						codigosGuardar.add(cod);
						preciosGuardar.add(precio);
					}
				}
			}

			try (RandomAccessFile rafWrite = new RandomAccessFile(ficheroBajas, "rw")) {
				rafWrite.setLength(0);

				for (int i = 0; i < codigosGuardar.size(); i++) {
					rafWrite.writeInt(codigosGuardar.get(i));
					rafWrite.writeFloat(preciosGuardar.get(i));
				}
			}

			System.out.println("Planta " + codigoARescatar + " eliminada de plantasBaja.dat.");

		} catch (IOException e) {
			System.err.println("Error al limpiar 'plantasBaja.dat'.");
		}
	}

	public static void cargarPlantasXML(File inputFile) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("planta");

			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;

					int codigo = Integer.parseInt(eElement.getElementsByTagName("codigo").item(0).getTextContent());
					String nombre = eElement.getElementsByTagName("nombre").item(0).getTextContent();
					String foto = eElement.getElementsByTagName("foto").item(0).getTextContent();
					String descripcion = eElement.getElementsByTagName("descripcion").item(0).getTextContent();

					listaPlantas.add(new Planta(codigo, nombre, foto, descripcion));
				}
			}

			System.out.printf("Fichero de plantas cargado. Número de plantas: %d", listaPlantas.size());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void escribirPlantaEnDat(Planta p) {
		try (RandomAccessFile raf = new RandomAccessFile("resources/plantas.dat", "rw")) {
			raf.seek(raf.length());

			raf.writeInt(p.getCodigo());
			raf.writeFloat(p.getPrecio());
			raf.writeInt(p.getStock());

		} catch (IOException e) {
			System.err.println("Error al escribir en 'plantas.dat'.");
		}
	}

	private static float buscarPrecioEnBajas(int codigoBuscar) {
		File ficheroBajas = new File("resources/plantasBaja.dat");
		if (!ficheroBajas.exists())
			return -1.0f;

		try (RandomAccessFile rafRead = new RandomAccessFile(ficheroBajas, "r")) {
			while (rafRead.getFilePointer() < rafRead.length()) {
				int cod = rafRead.readInt();
				float precio = rafRead.readFloat();
				if (cod == codigoBuscar) {
					return precio;
				}
			}
		} catch (IOException e) {
			System.err.println("Error al leer 'plantasBaja.dat': ");
		}
		return -1.0f;
	}

	public static void revisarCarpeta(String nombreCarpeta) {

		File carpeta = new File(nombreCarpeta);

		if (!carpeta.exists()) {
			System.out.printf("La carpeta %s no existe, por lo que se va a crear ahora.", nombreCarpeta);

			boolean creada = carpeta.mkdir();

			if (creada) {
				System.out.printf("Carpeta %s creada con éxito.", nombreCarpeta);
			} else {
				System.err.printf("No se pudo crear la carpeta %s.", nombreCarpeta);
			}
		}
	}

	public static void escribirBajaPlanta(int codigo, float precio) {
		try (RandomAccessFile raf = new RandomAccessFile("resources/plantasBaja.dat", "rw")) {
			raf.seek(raf.length());

			raf.writeInt(codigo);
			raf.writeFloat(precio);

		} catch (IOException e) {
			System.err.println("Error al escribir en plantasBaja.dat.");
		}
	}

	public static Empleado validarUsuario(int id, String password) {
		for (Empleado emp : listaEmpleados) {
			if (emp.getIdentificacion() == id && emp.getPassword().equals(password)) {
				return emp;
			}
		}
		return null;
	}

	private static String validarPassword(Scanner sc) {
		while (true) {
			System.out.print("Introduce la contraseña (entre 5 y 7 caracteres): ");
			String pass = sc.next();
			if (pass.length() >= 5 && pass.length() <= 7) {
				return pass;
			} else {
				System.err.println("Error: La contraseña debe tener entre 5 y 7 caracteres.");
			}
		}
	}

	private static String validarCargo(Scanner sc) {
		while (true) {
			System.out.print("Introduce el cargo (vendedor / gestor): ");
			String cargo = sc.next().toLowerCase();
			if (cargo.equals("vendedor") || cargo.equals("gestor")) {
				return cargo;
			} else {
				System.err.println("Error: El cargo solo puede ser 'vendedor' o 'gestor'.");
			}
		}
	}

	public static float validarFloat(Scanner sc, String mensaje) {
		while (true) {
			try {
				System.out.print(mensaje);
				float valor = sc.nextFloat();
				return valor;
			} catch (InputMismatchException e) {
				System.err.println("Error: Debes introducir un NÚMERO decimal (ej: 12,5).");
				sc.next();
			}
		}
	}

	public static int validarInt(Scanner sc, String mensaje) {
		while (true) {
			try {
				System.out.print(mensaje);
				int valor = sc.nextInt();
				return valor;

			} catch (InputMismatchException e) {
				System.err.println("Error. Debes introducir un número entero.");
				sc.next();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void cargarEmpleados() {
		try (FileInputStream fis = new FileInputStream("resources/empleado.dat");
				ObjectInputStream ois = new ObjectInputStream(fis)) {
			listaEmpleados = (ArrayList<Empleado>) ois.readObject();

			System.out.println("Fichero de empleados cargado correctamente.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void guardarEmpleados() {
		try (FileOutputStream fos = new FileOutputStream("resources/empleado.dat");
				ObjectOutputStream oos = new ObjectOutputStream(fos)) {

			oos.writeObject(listaEmpleados);

		} catch (IOException e) {
			System.err.println("Error al guardar 'empleado.dat'.");
		}
	}

	public static void cargarRecursos() {
		File empleados = new File("resources/empleado.dat");
		File plantasXML = new File("resources/plantas.xml");
		File plantasDAT = new File("resources/plantas.dat");

		if (!empleados.exists() || !plantasXML.exists() || !plantasDAT.exists()) {
			System.err.println("Error. Faltan ficheros.");
			return;
		}
		cargarEmpleados();
		cargarPlantasXML(plantasXML);
	}
}