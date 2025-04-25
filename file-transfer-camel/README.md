# Proyecto BioNet - Integración de Resultados de Laboratorios

Este proyecto utiliza **Apache Camel** y **PostgreSQL** para integrar archivos `.csv` de diferentes laboratorios clínicos hacia una base de datos unificada.

## 🛠 Estructura del proyecto


## 📦 Requisitos

- Java 11+
- Maven
- PostgreSQL
- Apache Camel
- JDBC Driver para PostgreSQL

## 🐘 Configuración de base de datos

1. Crea la base de datos:

```bash
createdb bionet_db

sudo -u postgres psql -d bionet_db -f scripts/init.sql

cd BioNet
mvn compile exec:java -Dexec.mainClass="com.techdistribuidora.camel.App"
