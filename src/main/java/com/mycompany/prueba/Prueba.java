/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.prueba;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.JDOMException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 *
 * @author imser
 */
public class Prueba {

    public static void main(String[] args) throws JsonProcessingException {

        // Lista de conceptos 
        Set<String> codconceptos = new HashSet<>();
        codconceptos.add("tipopeaje");

        List<LinkedHashMap<String, Object>> facturas = leerFacturas("src\\main\\java\\com\\mycompany\\prueba\\facturas.xml", codconceptos);

        // Convertir el conjunto de codconceptos a un array
        String[] codconceptoArray = codconceptos.toArray(new String[0]);

        CSV(codconceptoArray);

        MongoDatabase database = Connection.getConnection();
        MongoCollection<org.bson.Document> collection = database.getCollection("facturas");

        // Insertar cada factura en la base de datos
        for (LinkedHashMap<String, Object> facturaMap : facturas) {
            org.bson.Document doc = new org.bson.Document(facturaMap);
            collection.insertOne(doc);
            System.out.println(doc.toJson());
        }
        Connection.closeConnection();

    }

    public static void CSV(String[] conceptos) {
        String archivoCSV = "src\\main\\java\\com\\mycompany\\prueba\\conceptos_y_peajes.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(archivoCSV))) {
            // Escribir los codconceptos únicos en el CSV
            writer.println(String.join(",", conceptos));

            writer.println(); // Nueva línea al final

            System.out.println("Archivo CSV creado exitosamente.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<LinkedHashMap<String, Object>> leerFacturas(String archivoXML, Set<String> codconceptoSet) {
        List<LinkedHashMap<String, Object>> facturas = new ArrayList<>();

        try {
            // Crear una instancia de SAXBuilder
            SAXBuilder saxBuilder = new SAXBuilder();

            // Parsear el archivo XML y obtener el documento
            org.jdom2.Document document = saxBuilder.build(new File(archivoXML));

            // Obtener el elemento raíz
            Element rootElement = document.getRootElement();

            // Obtener todos los elementos "factura"
            List<Element> listaFacturas = rootElement.getChildren("factura");

            // Recorrer todos los elementos "factura"
            for (Element elementoFactura : listaFacturas) {
                LinkedHashMap<String, Object> facturaMap = new LinkedHashMap<>();

                // Procesar recursivamente los hijos de la factura
                procesarElementosHijos(elementoFactura, facturaMap, codconceptoSet);

                facturas.add(facturaMap);
            }
        } catch (JDOMException | IOException e) {
            e.printStackTrace();
        }

        return facturas;
    }

    private static void procesarElementosHijos(Element elemento, LinkedHashMap<String, Object> map, Set<String> codconceptoSet) {
        List<Element> hijos = elemento.getChildren();
        for (Element hijo : hijos) {
            String nombreHijo = hijo.getName();

            if (nombreHijo.equals("codconcepto")) {
                codconceptoSet.add(hijo.getValue());
            }
            if (hijo.getChildren().isEmpty()) {
                // Si el elemento hijo no tiene hijos, lo añadimos como texto
                agregarAlMapa(map, nombreHijo, hijo.getValue());
            } else {
                // Si el elemento hijo tiene más hijos, creamos un nuevo LinkedHashMap y llamamos recursivamente
                LinkedHashMap<String, Object> hijoMap = new LinkedHashMap<>();
                procesarElementosHijos(hijo, hijoMap, codconceptoSet);
                agregarAlMapa(map, nombreHijo, hijoMap);
            }
        }
    }

    private static void agregarAlMapa(LinkedHashMap<String, Object> map, String key, Object value) {
        if (map.containsKey(key)) {
            Object existente = map.get(key);
            if (existente instanceof List) {
                ((List<Object>) existente).add(value);
            } else {
                List<Object> lista = new ArrayList<>();
                lista.add(existente);
                lista.add(value);
                map.put(key, lista);
            }
        } else {
            map.put(key, value);
        }
    }

}
