-- scripts/init.sql

-- Tabla principal para resultados
CREATE TABLE resultados_examenes (
    id SERIAL PRIMARY KEY,
    laboratorio_id INT NOT NULL,
    paciente_id INT NOT NULL,
    tipo_examen TEXT NOT NULL,
    resultado TEXT NOT NULL,
    fecha_examen DATE NOT NULL,
    UNIQUE(paciente_id, tipo_examen, fecha_examen)
);

-- Tabla de auditoría
CREATE TABLE log_cambios_resultados (
    id SERIAL PRIMARY KEY,
    operacion TEXT NOT NULL,
    paciente_id INT NOT NULL,
    tipo_examen TEXT NOT NULL,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trigger para registrar operaciones
CREATE OR REPLACE FUNCTION log_resultado_changes()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO log_cambios_resultados(operacion, paciente_id, tipo_examen)
    VALUES (TG_OP, NEW.paciente_id, NEW.tipo_examen);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_resultado
AFTER INSERT OR UPDATE ON resultados_examenes
FOR EACH ROW EXECUTE FUNCTION log_resultado_changes();
