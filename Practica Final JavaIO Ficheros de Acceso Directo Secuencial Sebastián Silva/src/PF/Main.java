package PF;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.InputMismatchException;
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
		File empleados = new File("resources/empleado.dat");
		File plantasXML = new File("resources/plantas.xml");
		File plantasDAT = new File("resources/plantas.dat");

		if (!empleados.exists() || !plantasXML.exists() || !plantasDAT.exists()) {
			System.err.println("Error. Faltan ficheros.");
			return;
		}
		System.out.println("Ficheros empleados.dat, plantas.xml y plantas.dat cargados correctamente.");

		cargarEmpleados();
		cargarPlantasXML(plantasXML);
		Scanner sc = new Scanner(System.in);

		System.out.println();
		System.out.println("Bienvenido al vivero.");
		int nIntentos = 0;

		do {

			int id = revisarInt(sc, "\tIntroduce tu ID: ");
			System.out.print("\tIntroduce tu contraseña: ");
			String pass = sc.next();

			Empleado empleadoLogueado = validarUsuario(id, pass);

			if (empleadoLogueado != null) {
				System.out.printf("Empleado %s ha accedido al sistema.", empleadoLogueado.getNombre());
				if (empleadoLogueado.getCargo().equalsIgnoreCase("vendedor")) {
					menuVendedor(sc, empleadoLogueado);
					break;
				} else if (empleadoLogueado.getCargo().equalsIgnoreCase("gestor")) {
					// menuGestor();
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

		sc.close();
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

	public static void revisarCarpeta(String nombreCarpeta) {

		File carpeta = new File(nombreCarpeta);

		if (!carpeta.exists()) {
			System.out.printf("La carpeta %s no existe, por lo que se va a crear ahora.", nombreCarpeta);

			boolean creada = carpeta.mkdir();

			if (creada) {
				System.out.printf("Carpeta %s creada con éxito.", nombreCarpeta);
			} else {
				System.err.printf("No se pudo crear la carpeta .%s", nombreCarpeta);
			}
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

					float precio = 0.0f;
					int stock = 0;

					listaPlantas.add(new Planta(codigo, stock, precio, nombre, foto, descripcion));
				}
			}

			System.out.printf("Fichero de plantas cargado. Número de plantas: %d", listaPlantas.size());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void menuVendedor(Scanner sc, Empleado eLogeado) {
		System.out.println("\nMenú de vendedor");

		int opcion;
		do {

			System.out.println("\n\t1 para mostrar el catálogo");
			System.out.println("\t2 para realizar una venta");
			System.out.println("\t3 para realizar una devolución");
			System.out.println("\t4 para cerrar sesión");

			opcion = revisarInt(sc, "Elige una opción: ");
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
				System.out.println("Saliendo...");
				break;
			default:
				System.err.println("Opción no válida. Inténtalo de nuevo (1-3).");
			}
		} while (opcion != 4);
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
		System.out.println("\n--- Nueva Venta ---");

		try {
			int codigo = revisarInt(sc, "Introduce el CÓDIGO de la planta a vender: ");

			if (codigo < 1 || codigo > listaPlantas.size()) {
				System.err.println("Error: Código de planta no existe.");
				return;
			}

			int cantidad = revisarInt(sc, "Introduce la CANTIDAD: ");

			if (cantidad <= 0) {
				System.err.println("Error: La cantidad debe ser positiva.");
				return;
			}

			try (RandomAccessFile raf = new RandomAccessFile("resources/plantas.dat", "rw")) {

				long posicion = (long) (codigo - 1) * 12;
				raf.seek(posicion);

				raf.readInt();
				float precio = raf.readFloat();
				int stockActual = raf.readInt();

				if (cantidad > stockActual) {
					System.err.printf("Error: No hay stock. Solo quedan %d unidades.\n", stockActual);
					return;
				}

				float total = precio * cantidad;
				String nombrePlanta = listaPlantas.get(codigo - 1).getNombre();

				System.out.println("\n--- RESUMEN DE COMPRA ---");
				System.out.printf("Planta: %s (Cód: %d)\n", nombrePlanta, codigo);
				System.out.printf("Cantidad: %d\n", cantidad);
				System.out.printf("Precio Unitario: %.2f €\n", precio);
				System.out.printf("TOTAL: %.2f €\n", total);
				System.out.print("¿Confirmar venta? (s/n): ");

				String confirmacion = sc.next();

				if (confirmacion.equalsIgnoreCase("s")) {
					int nuevoStock = stockActual - cantidad;

					raf.seek(posicion + 8);
					raf.writeInt(nuevoStock);

					System.out.println("¡Venta realizada! Stock actualizado.");

					generarTicket(empleado, codigo, nombrePlanta, cantidad, precio, total);
				} else {
					System.out.println("Venta cancelada.");
				}
			}

		} catch (IOException e) {
			System.err.println("Error de E/S al procesar la venta: " + e.getMessage());
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
			pw.println("Empleado que ha atendido: " + empleado.getIdentificacion());
			pw.println("Nombre del empleado: " + empleado.getNombre());
			pw.println("Fecha de venta: " + LocalDate.now());
			pw.println();
			pw.println("Producto\tCant.\tPrecio Ud.\tTotal");
			pw.printf("%s (Cód:%d)\t%d\t%.2f€\t\t%.2f€\n", nomProd, codProd, cantidad, precioUd, total);
			pw.println();
			pw.println("TOTAL A PAGAR: " + String.format("%.2f €", total));

			System.out.println("Ticket " + ticketNum + ".txt generado con éxito.");

		} catch (IOException e) {
			System.err.println("Error al generar el ticket: " + e.getMessage());
		}
	}

	public static int revisarInt(Scanner sc, String mensaje) {
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

	public static int obtenerSiguienteNumeroTicket() {
		revisarCarpeta("TICKETS");
		revisarCarpeta("DEVOLUCIONES");

		int maxTickets = buscarMaxNumEnCarpeta("TICKETS");
		int maxDevoluciones = buscarMaxNumEnCarpeta("DEVOLUCIONES");

		int maxGlobal = Math.max(maxTickets, maxDevoluciones);

		return maxGlobal + 1;
	}

	public static void realizarDevolucion(Scanner sc) {
		System.out.println("\n--- Procesar Devolución ---");

		int numTicket = revisarInt(sc, "Introduce el número de ticket a devolver: ");

		String nombreFicheroOriginal = "TICKETS/" + numTicket + ".txt";
		String nombreFicheroDevuelto = "DEVOLUCIONES/" + numTicket + ".txt";

		File ficheroTicketOriginal = new File(nombreFicheroOriginal);
		File ficheroTicketDevuelto = new File(nombreFicheroDevuelto);

		if (!ficheroTicketOriginal.exists()) {
			System.err.println("Error: El ticket " + numTicket + ".txt no existe en la carpeta TICKETS.");
			return;
		}

		if (ficheroTicketDevuelto.exists()) {
			System.err.println("Error: El ticket " + numTicket + ".txt ya fue procesado como una devolución.");
			return;
		}

		System.out.println("Ticket " + numTicket + ".txt encontrado. Procesando...");

		int codigoPlanta = -1;
		int cantidadDevolver = -1;
		ArrayList<String> contenidoTicket = new ArrayList<>();

		try {
			try (FileReader fr = new FileReader(ficheroTicketOriginal); BufferedReader br = new BufferedReader(fr)) {

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
				System.err.println("Error: El formato del ticket " + numTicket + ".txt es incorrecto.");
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

			try (FileWriter fw = new FileWriter(ficheroTicketDevuelto); PrintWriter pw = new PrintWriter(fw)) {
				for (String linea : contenidoTicket) {
					pw.println(linea);
				}
				pw.println("---------------------------------");
				pw.println("--- TICKET DEVUELTO ---");
				pw.println("--- STOCK RESTAURADO ---");
			}

			ficheroTicketOriginal.delete();

			System.out.println("¡Devolución completada!");
			System.out.printf("Se han devuelto %d unidades de la planta Cód:%d al stock.\n", cantidadDevolver,
					codigoPlanta);
			System.out.println("Ticket " + numTicket + ".txt movido a DEVOLUCIONES.");

		} catch (IOException e) {
			System.err.println("Error de E/S al procesar la devolución: " + e.getMessage());
		} catch (NumberFormatException e) {
			System.err.println("Error: No se pudo 'parsear' el contenido del ticket. Formato corrupto.");
		}
	}

	private static int buscarMaxNumEnCarpeta(String nombreCarpeta) {
		File carpeta = new File(nombreCarpeta);

		if (!carpeta.exists()) {
			return 0;
		}

		File[] ficheros = carpeta.listFiles();
		int maxNum = 0;

		if (ficheros != null) {
			for (File f : ficheros) {
				String nombre = f.getName();
				if (nombre.endsWith(".txt")) {
					try {
						String numStr = nombre.substring(0, nombre.lastIndexOf('.'));
						int num = Integer.parseInt(numStr);
						if (num > maxNum) {
							maxNum = num;
						}
					} catch (Exception e) {
					}
				}
			}
		}
		return maxNum;
	}
}
