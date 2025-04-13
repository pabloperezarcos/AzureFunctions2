package com.function;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Proveedor para GraphQL de Roles.
 * Carga el esquema 'schema.graphqls' y define los resolvers que conectan con
 * RolDataFetcher.
 */
public class RolesGraphQLProvider {

    private GraphQL graphQL;

    public RolesGraphQLProvider() {
        init();
    }

    private void init() {
        try {
            // Cargar el schema de roles
            InputStream schemaStream = getClass().getResourceAsStream("/schema.graphqls");
            if (schemaStream == null) {
                throw new RuntimeException("No se encontró 'schema.graphqls' para roles en resources.");
            }
            Reader reader = new InputStreamReader(schemaStream);
            TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(reader);

            // Construir el wiring
            RuntimeWiring wiring = buildRuntimeWiring();

            // Crear el esquema ejecutable
            SchemaGenerator schemaGenerator = new SchemaGenerator();
            GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, wiring);
            this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar RolesGraphQLProvider: " + e.getMessage(), e);
        }
    }

    private RuntimeWiring buildRuntimeWiring() {
        // Instancia de RolDataFetcher que hará la conexión a la BD y la lógica CRUD
        RolDataFetcher rolDF = new RolDataFetcher();

        return RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder
                        .dataFetcher("rol", environment -> {
                            int id = Integer.parseInt(environment.getArgument("id"));
                            return rolDF.obtenerRolPorId(id);
                        })
                        .dataFetcher("roles", environment -> {
                            return rolDF.listarRoles();
                        }))
                .type("Mutation", builder -> builder
                        .dataFetcher("crearRol", environment -> {
                            String rolNombre = environment.getArgument("rol");
                            return rolDF.crearRol(rolNombre);
                        })
                        .dataFetcher("actualizarRol", environment -> {
                            int id = Integer.parseInt(environment.getArgument("id"));
                            String rolNombre = environment.getArgument("rol");
                            return rolDF.actualizarRol(id, rolNombre);
                        })
                        .dataFetcher("eliminarRol", environment -> {
                            int id = Integer.parseInt(environment.getArgument("id"));
                            return rolDF.eliminarRol(id);
                        }))
                .build();
    }

    public GraphQL getGraphQL() {
        return graphQL;
    }
}
