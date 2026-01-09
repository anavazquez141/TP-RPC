# Sistema de Donaciones Distribuido – “Empuje Comunitario”

Proyecto académico grupal desarrollado para la materia  
**Desarrollo de Software en Sistemas Distribuidos**  

## 🎯 Descripción general

“Empuje Comunitario” es un **sistema web distribuido** orientado a la gestión de **usuarios, donaciones y eventos solidarios** para una ONG ficticia.

El sistema fue diseñado siguiendo una **arquitectura de microservicios**, permitiendo la comunicación entre componentes mediante distintos estilos de integración y protocolos utilizados en entornos reales.

## 🧩 Funcionalidades principales

- 🔐 **Autenticación y autorización por roles**
- 👤 **ABM de usuarios** con contraseñas encriptadas
- 📦 **Gestión de donaciones** por categorías e inventario
- 📅 **Administración de eventos solidarios**
- 📝 Registro de participantes en eventos
- 📊 **Auditoría de acciones** realizadas en el sistema

## ⚙️ Arquitectura y comunicación

El sistema implementa una arquitectura distribuida basada en:

- **Microservicios independientes**
- Comunicación mediante:
  - gRPC
  - REST API
  - GraphQL
  - SOAP
- **Mensajería asincrónica** con Apache Kafka
- Persistencia en **base de datos relacional**

## 🛠️ Tecnologías utilizadas

### Backend
- Java  
- Spring Boot  
- Maven  

### Comunicación y servicios
- gRPC  
- REST API  
- GraphQL  
- SOAP  

### Mensajería
- Apache Kafka  
- Kafbat  

### Otros
- Swagger (documentación de APIs)  
- Python (servicios complementarios)

## 👩‍💻 Rol en el proyecto

**Desarrolladora Backend / Full Stack**

Participé en:
- Desarrollo de servicios backend
- Implementación de endpoints y comunicación entre microservicios
- Lógica de negocio y persistencia de datos
- Integración de distintos protocolos de comunicación

