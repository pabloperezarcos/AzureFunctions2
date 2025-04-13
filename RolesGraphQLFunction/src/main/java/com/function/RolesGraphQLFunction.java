package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

/**
 * Azure Function que expone un endpoint GraphQL para la entidad "roles".
 * Recibe peticiones HTTP POST con un JSON que contiene "query" y opcionalmente
 * "variables".
 */
public class RolesGraphQLFunction {

    // Instancia de GraphQL inicializada en RolesGraphQLProvider
    private static final GraphQL graphQL = new RolesGraphQLProvider().getGraphQL();

    /**
     * Función "RolesGraphQLHandler" que escucha en /api/rolesgraphql (configurable
     * en 'route').
     * Ajusta el 'route' según tus preferencias.
     */
    @FunctionName("RolesGraphQLHandler")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "rolesgraphql") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        try {
            // Obtener el cuerpo de la solicitud
            String requestBody = request.getBody()
                    .orElseThrow(() -> new IllegalArgumentException("El cuerpo de la solicitud es obligatorio."));

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(requestBody, Map.class);

            String query = (String) payload.get("query");
            Map<String, Object> variables = (Map<String, Object>) payload.getOrDefault("variables", Map.of());

            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("El campo 'query' es obligatorio en el JSON.");
            }

            // Construir y ejecutar la consulta GraphQL
            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                    .query(query)
                    .variables(variables)
                    .build();

            ExecutionResult executionResult = graphQL.execute(executionInput);

            // Convertir el resultado a JSON
            String jsonResult = mapper.writeValueAsString(executionResult.toSpecification());

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(jsonResult)
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Error en RolesGraphQLFunction: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
