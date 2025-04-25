package com.techdistribuidora.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.component.jdbc.JdbcComponent;

import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class App {

    public static DataSource setupDataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl("jdbc:postgresql://localhost:5432/bionet_db");
        ds.setUsername("postgres");
        ds.setPassword("root");
        return ds;
    }

    public static void main(String[] args) throws Exception {
        CamelContext context = new DefaultCamelContext();

        // Configurar componente JDBC
        JdbcComponent jdbc = new JdbcComponent();
        jdbc.setDataSource(setupDataSource());
        context.addComponent("jdbc", jdbc);

        context.addRoutes(new RouteBuilder() {
            public void configure() {
                from("file:input-labs?noop=true")
                    .routeId("transferencia-labs")
                    .log("📂 Archivo detectado: ${header.CamelFileName}")
                    .process(new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            String filePath = exchange.getIn().getHeader("CamelFileAbsolutePath", String.class);
                            List<String> lines = Files.readAllLines(Paths.get(filePath));

                            if (lines.isEmpty() || !lines.get(0).toLowerCase().contains("paciente_id")) {
                                throw new Exception("❌ Encabezado inválido");
                            }

                            StringBuilder sqlBatch = new StringBuilder();
                            int registrosProcesados = 0;

                            for (int i = 1; i < lines.size(); i++) {
                                String[] data = lines.get(i).split(",");
                                if (data.length < 5) continue;

                                int labId = Integer.parseInt(data[0].trim());
                                int pacienteId = Integer.parseInt(data[1].trim());
                                String tipoExamen = data[2].trim();
                                String resultado = data[3].trim();
                                String fecha = data[4].trim();

                                String sql = String.format(
                                    "INSERT INTO resultados_examenes (laboratorio_id, paciente_id, tipo_examen, resultado, fecha_examen) " +
                                    "VALUES (%d, %d, '%s', '%s', '%s') " +
                                    "ON CONFLICT (paciente_id, tipo_examen, fecha_examen) DO UPDATE " +
                                    "SET resultado = EXCLUDED.resultado;",
                                    labId, pacienteId, tipoExamen, resultado, fecha
                                );

                                sqlBatch.append(sql).append("\n");
                                registrosProcesados++;
                            }

                            exchange.getIn().setBody(sqlBatch.toString());
                            exchange.setProperty("registrosProcesados", registrosProcesados);  // <-- Guardamos para log
                        }
                    })
                    .to("jdbc:dataSource")
                    .log("✅ Datos insertados exitosamente en la base de datos.")
                    .to("file:processed")
                    .onException(Exception.class)
                        .handled(true)
                        .log("❌ Error procesando archivo: ${exception.message}")
                        .to("file:error")
                    .end();
            }
        });

        context.start();
        System.out.println("🚀 Camel ejecutando. Presiona Ctrl+C para detener.");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("🛑 Deteniendo contexto...");
                context.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        Thread.currentThread().join();
    }
}
