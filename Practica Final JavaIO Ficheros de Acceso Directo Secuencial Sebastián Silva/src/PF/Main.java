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
import java.util.Random;
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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

			int id = validarInt(sc, "\tIntroduce tu ID: ");
			System.out.print("\tIntroduce tu contraseña: ");
			String pass = sc.next();

			Empleado empleadoLogueado = validarUsuario(id, pass);

			if (empleadoLogueado != null) {
				System.out.printf("Empleado %s ha accedido al sistema.", empleadoLogueado.getNombre());
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

		sc.close();
	}

	
	public static void menuVendedor(Scanner sc, Empleado eLogeado) {
		System.out.println("\nMenú de vendedor");

		int opcion;
		do {

			System.out.println("\n\t1 para mostrar el catálogo");
			System.out.println("\t2 para realizar una venta");
			System.out.println("\t3 para realizar una devolución");
			System.out.println("\t4 para cerrar sesión");

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
			int codigo = validarInt(sc, "Introduce el CÓDIGO de la planta a vender: ");

			if (codigo < 1 || codigo > listaPlantas.size()) {
				System.err.println("Error: Código de planta no existe.");
				return;
			}

			int cantidad = validarInt(sc, "Introduce la CANTIDAD: ");

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

	public static void realizarDevolucion(Scanner sc) {
		System.out.println("\n--- Procesar Devolución ---");

		int numTicket = validarInt(sc, "Introduce el número de ticket a devolver: ");

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

	public static void menuGestor(Scanner sc, Empleado eLogeado) {
		System.out.println("\nMenú de Gestor");

		int opcion;
		do {
			System.out.println("\n\t1 para dar de alta una planta");
			System.out.println("\t2 para dar de baja una planta");
			System.out.println("\t3 para modificar stock o precio de planta");
			System.out.println("\t4 para dar de alta un empleado");
			System.out.println("\t5 para dar de baja un empleado");
			System.out.println("\t6 para ver estadísticas de ventas");
			System.out.println("\t7 para cerrar sesión");

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
				verEstadisticas();
				System.out.println("Opción 6 en construcción...");
				break;
			case 7:
				System.out.println("Cerrando sesión... ¡Hasta pronto!");
				break;
			default:
				System.err.println("Opción no válida. Inténtalo de nuevo (1-7).");
			}
		} while (opcion != 7);
	}
	
	public static void darAltaPlanta(Scanner sc) {
		System.out.println("\n--- Alta de Nueva Planta ---");
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

		System.out.println("Planta añadida a la lista en memoria.");

		escribirPlantaEnDat(nuevaPlanta);

		escribirPlantasXML();

		System.out.println("\n¡Planta dada de alta con éxito en ambos ficheros!");
		System.out.println(nuevaPlanta.toString());
	}
	
	public static void darBajaPlanta(Scanner sc) {
		System.out.println("\n--- Baja de Planta ---");

		int codigo = validarInt(sc, "Introduce el CÓDIGO de la planta a dar de baja: ");
		if (codigo < 1 || codigo > listaPlantas.size()) {
			System.err.println("Error: Código de planta no existe.");
			return;
		}

		try (RandomAccessFile raf = new RandomAccessFile("resources/plantas.dat", "rw")) {
			long posicion = (long) (codigo - 1) * 12;
			raf.seek(posicion);

			raf.readInt();
			float precioOriginal = raf.readFloat();
			int stockOriginal = raf.readInt();

			if (precioOriginal == 0.0f && stockOriginal == 0) {
				System.out.println("AVISO: La planta " + codigo + " ya estaba dada de baja.");
				return;
			}

			raf.seek(posicion + 4);
			raf.writeFloat(0.0f);
			raf.writeInt(0);

			escribirBajaPlanta(codigo, precioOriginal);

			System.out.println("¡Planta dada de baja con éxito!");
			System.out.println("Stock y precio puestos a 0 en plantas.dat.");
			System.out.println("Datos de recuperación guardados en plantasBaja.dat.");

		} catch (IOException e) {
			System.err.println("Error de E/S al dar de baja la planta: " + e.getMessage());
		}
	}
	
	public static void darAltaEmpleado(Scanner sc) {
		System.out.println("\n--- Alta de Nuevo Empleado ---");
		sc.nextLine(); 

		System.out.print("Introduce el nombre: ");
		String nombre = sc.nextLine();

		String pass = validarPassword(sc);

		String cargo = validarCargo(sc);

		int id = generarIDEmpleado();
		System.out.println("ID de 4 dígitos generado: " + id);

		Empleado nuevoEmp = new Empleado(id, nombre, pass, cargo);
		listaEmpleados.add(nuevoEmp);

		guardarEmpleados();

		System.out.println("\n¡Empleado " + nombre + " (ID: " + id + ") dado de alta con éxito!");
	}

    public static void darBajaEmpleado(Scanner sc) {
        System.out.println("\n--- Baja de Empleado ---");
        
        int idBaja = validarInt(sc, "Introduce el ID del empleado a dar de baja: ");

        Empleado empleadoADespedir = null;
        for (Empleado emp : listaEmpleados) {
            if (emp.getIdentificacion() == idBaja) {
                empleadoADespedir = emp;
                break;
            }
        }

        if (empleadoADespedir == null) {
            System.err.println("Error: No se ha encontrado ningún empleado con el ID " + idBaja);
            return;
        }
        
        guardarEmpleadoBaja(empleadoADespedir);
        
        listaEmpleados.remove(empleadoADespedir);
        
        guardarEmpleados();
        
        System.out.println("¡Empleado " + empleadoADespedir.getNombre() + " (ID: " + idBaja + ") dado de baja!");
        System.out.println("Movido a BAJA/empleadosBaja.dat");
    }
    
    public static void modificarPlanta(Scanner sc) {
		System.out.println("\n--- Modificar Planta (o Rescatar) ---");

		int codigo = validarInt(sc, "Introduce el CÓDIGO de la planta a modificar: ");
		if (codigo < 1 || codigo > listaPlantas.size()) {
			System.err.println("Error: Código de planta no existe.");
			return;
		}

		try (RandomAccessFile raf = new RandomAccessFile("resources/plantas.dat", "rw")) {
			long posicion = (long) (codigo - 1) * 12;
			raf.seek(posicion);

			raf.readInt();
			float precioActual = raf.readFloat();
			raf.readInt();

			System.out.println("Datos actuales: Precio=" + precioActual);

			float nuevoPrecio = validarFloat(sc, "Introduce el NUEVO precio: ");
			int nuevoStock = validarInt(sc, "Introduce el NUEVO stock: ");

			raf.seek(posicion + 4);
			raf.writeFloat(nuevoPrecio);
			raf.writeInt(nuevoStock);

			System.out.println("¡Planta actualizada en plantas.dat!");

			if (precioActual == 0.0f && nuevoPrecio > 0.0f) {
				System.out.println("Planta " + codigo + " ha sido 'rescatada'.");
				eliminarPlantaDeBajas(codigo);
			}

		} catch (IOException e) {
			System.err.println("Error de E/S al modificar la planta: " + e.getMessage());
		}
	}
    
    public static void verEstadisticas() {
        System.out.println("\n--- Estadísticas de Ventas ---");

        File carpetaTickets = new File("TICKETS");
        File[] listaTickets = carpetaTickets.listFiles();

        if (listaTickets == null || listaTickets.length == 0) {
            System.out.println("No hay tickets de venta activos para generar estadísticas.");
            return;
        }

        double totalRecaudado = 0.0;
        HashMap<Integer, Integer> ventasPorPlanta = new HashMap<>();

        for (File ticket : listaTickets) {
            if (ticket.getName().endsWith(".txt")) {
                try (FileReader fr = new FileReader(ticket);
                     BufferedReader br = new BufferedReader(fr)) {
                    
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
                    System.err.println("Error al leer el ticket " + ticket.getName() + ": " + e.getMessage());
                }
            }
        } 
        System.out.printf("Total Recaudado (solo ventas activas): %.2f €\n", totalRecaudado);

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
            
            System.out.println("Planta más vendida: " + nombrePlanta + " (Cód: " + codPlantaMasVendida + ")");
            System.out.println("Unidades vendidas: " + maxVentas);
        }
    }

    @SuppressWarnings("unchecked")
	public static void guardarEmpleadoBaja(Empleado empleadoBaja) {
        revisarCarpeta("BAJA");  
        
        String ficheroBajas = "BAJA/empleadosBaja.dat";
        ArrayList<Empleado> listaDeBajas = new ArrayList<>();
        File f = new File(ficheroBajas);

        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(ficheroBajas);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                
                listaDeBajas = (ArrayList<Empleado>) ois.readObject();
                
            } catch (Exception e) {
                System.err.println("Error al leer 'empleadosBaja.dat': " + e.getMessage());
            }
        }

        listaDeBajas.add(empleadoBaja);

        try (FileOutputStream fos = new FileOutputStream(ficheroBajas);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            
            oos.writeObject(listaDeBajas);
            
        } catch (IOException e) {
            System.err.println("Error al guardar en 'empleadosBaja.dat': " + e.getMessage());
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

	


	public static void escribirPlantaEnDat(Planta p) {
		try (RandomAccessFile raf = new RandomAccessFile("resources/plantas.dat", "rw")) {
			raf.seek(raf.length());

			raf.writeInt(p.getCodigo());
			raf.writeFloat(p.getPrecio());
			raf.writeInt(p.getStock());

		} catch (IOException e) {
			System.err.println("Error al escribir en plantas.dat: " + e.getMessage());
		}
	}

	public static void escribirPlantasXML() {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();

			Element rootElement = doc.createElement("plantas");
			doc.appendChild(rootElement);

			for (Planta p : listaPlantas) {
				Element ePlanta = doc.createElement("planta");
				rootElement.appendChild(ePlanta);

				Element eCodigo = doc.createElement("codigo");
				eCodigo.appendChild(doc.createTextNode(String.valueOf(p.getCodigo())));
				ePlanta.appendChild(eCodigo);

				Element eNombre = doc.createElement("nombre");
				eNombre.appendChild(doc.createTextNode(p.getNombre()));
				ePlanta.appendChild(eNombre);

				Element eFoto = doc.createElement("foto");
				eFoto.appendChild(doc.createTextNode(p.getFoto()));
				ePlanta.appendChild(eFoto);

				Element eDesc = doc.createElement("descripcion");
				eDesc.appendChild(doc.createTextNode(p.getDescripcion()));
				ePlanta.appendChild(eDesc);
			}

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();

			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File("resources/plantas.xml"));

			transformer.transform(source, result);

		} catch (ParserConfigurationException | javax.xml.transform.TransformerException e) {
			System.err.println("Error al sobrescribir plantas.xml: " + e.getMessage());
		}
	}

	

	public static void escribirBajaPlanta(int codigo, float precio) {
		try (RandomAccessFile raf = new RandomAccessFile("resources/plantasBaja.dat", "rw")) {
			raf.seek(raf.length());

			raf.writeInt(codigo);
			raf.writeFloat(precio);

		} catch (IOException e) {
			System.err.println("Error al escribir en plantasBaja.dat: " + e.getMessage());
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
			System.err.println("Error al limpiar 'plantasBaja.dat': " + e.getMessage());
		}
	}

	


	private static int generarIDEmpleado() {
		Random r = new Random();
		return 1000 + r.nextInt(9000);
	}

	public static void guardarEmpleados() {
		try (FileOutputStream fos = new FileOutputStream("resources/empleado.dat");
				ObjectOutputStream oos = new ObjectOutputStream(fos)) {

			oos.writeObject(listaEmpleados);

		} catch (IOException e) {
			System.err.println("Error al guardar 'empleado.dat': " + e.getMessage());
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
}
