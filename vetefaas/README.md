# VeteFaaS - Backend for Frontend (BFF) para Sistema Veterinario

## Descripción del Proyecto
VeteFaaS es un microservicio BFF desarrollado con Spring Boot, diseñado para realizar las operaciones de gestión de usuarios y roles en el contexto de un sistema veterinario. Este servicio se comunica con funciones serverless (Azure Functions) para ejecutar las operaciones CRUD en una base de datos Oracle.

## Funcionalidades Clave
- **Crear Usuario:** Expone un endpoint REST que recibe la información del usuario (nombre y email) y delega la creación a una Azure Function.
- **Asignar Rol:** Expone un endpoint REST que recibe `userId` y `rolId` (por query o body) para asignar un rol a un usuario mediante otra Azure Function.
- **Integración con Oracle:** La conexión a la base de datos se configura mediante `application.properties` para operaciones de lectura y verificación (las inserciones se realizan en las funciones).
- **Comunicación via REST:** Los endpoints devuelven respuestas en formato JSON.
- **Dockerización:** Incluye un Dockerfile para facilitar el despliegue y pruebas locales.

## Requisitos
- Java 17
- Maven
- Git
- Docker (opcional, para despliegue)

