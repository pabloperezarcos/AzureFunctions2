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
 * Función Azure que expone un endpoint GraphQL para la entidad Usuario.
 * Este endpoint procesa peticiones HTTP POST con un payload JSON que contiene
 * la consulta GraphQL y, opcionalmente, las variables.
 */
public class GraphQLFunction {

    // Inicializa GraphQL usando nuestro proveedor que define el esquema y los
    // resolvers.
    private static final GraphQL graphQL = new GraphQLProvider().getGraphQL();

    @FunctionName("GraphQLHandler")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "graphql") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        try {
            String requestBody = request.getBody()
                    .orElseThrow(() -> new IllegalArgumentException("El cuerpo de la solicitud es obligatorio."));

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(requestBody, Map.class);

            String query = (String) payload.get("query");
            Map<String, Object> variables = (Map<String, Object>) payload.getOrDefault("variables", Map.of());

            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("El campo 'query' es obligatorio.");
            }

            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                    .query(query)
                    .variables(variables)
                    .build();

            ExecutionResult executionResult = graphQL.execute(executionInput);
            String jsonResult = mapper.writeValueAsString(executionResult.toSpecification());

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(jsonResult)
                    .build();
        } catch (Exception e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
