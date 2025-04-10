package com.techdistribuidora.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class App {
    public static void main(String[] args) throws Exception {
        CamelContext context = new DefaultCamelContext();

        context.addRoutes(new RouteBuilder() {
            public void configure() {
                from("file:input?noop=true")
                    .log("🔎 Archivo detectado: ${header.CamelFileName}")
                    .process(new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            String filePath = exchange.getIn().getHeader("CamelFileAbsolutePath", String.class);
                            List<String> lines = Files.readAllLines(Paths.get(filePath));

                            // Validación de encabezado
                            if (lines.isEmpty() || !lines.get(0).toLowerCase().contains("cliente")) {
                                throw new Exception("❌ Archivo sin encabezado válido. Se cancela la transferencia.");
                            }

                            // Obtener tipo de cliente de la primera línea de datos (línea 2)
                            if (lines.size() < 2) {
                                throw new Exception("❌ Archivo no contiene datos. Se cancela la transferencia.");
                            }

                            String[] columns = lines.get(1).split(",");
                            if (columns.length < 3) {
                                throw new Exception("❌ Formato de datos incorrecto.");
                            }

                            String tipoCliente = columns[2].trim(); // VIP, regular, etc.
                            exchange.setProperty("tipoCliente", tipoCliente);
                        }
                    })
                    .log("✅ Archivo validado. Cliente: ${exchangeProperty.tipoCliente}")
                    .toD("file:output/${exchangeProperty.tipoCliente}/")
                    .log("📁 Archivo movido a output/${exchangeProperty.tipoCliente}/: ${header.CamelFileName}");
            }
        });

        context.start();
        System.out.println("🚀 Ruta Camel corriendo. Presiona Ctrl+C para detener.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("🛑 Deteniendo contexto Camel...");
                context.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        Thread.currentThread().join();
    }
}
