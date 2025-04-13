package com.function;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Proveedor de GraphQL que carga el esquema y define los resolvers.
 */
public class GraphQLProvider {

    private GraphQL graphQL;

    public GraphQLProvider() {
        init();
    }

    private void init() {
        try {
            InputStream schemaStream = getClass().getResourceAsStream("/schema.graphqls");
            if (schemaStream == null) {
                throw new RuntimeException("No se encontró 'schema.graphqls' en resources.");
            }
            Reader streamReader = new InputStreamReader(schemaStream);
            TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(streamReader);
            RuntimeWiring wiring = buildRuntimeWiring();
            GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
            graphQL = GraphQL.newGraphQL(graphQLSchema).build();
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar GraphQL: " + e.getMessage(), e);
        }
    }

    private RuntimeWiring buildRuntimeWiring() {
        UsuarioDataFetcher usuarioDF = new UsuarioDataFetcher();

        return RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder
                        .dataFetcher("usuario", environment -> {
                            int id = Integer.parseInt(environment.getArgument("id"));
                            return usuarioDF.obtenerUsuarioPorId(id);
                        })
                        .dataFetcher("usuarios", environment -> {
                            return usuarioDF.listarUsuarios();
                        }))
                .type("Mutation", builder -> builder
                        .dataFetcher("crearUsuario", environment -> {
                            String nombre = environment.getArgument("nombre");
                            String email = environment.getArgument("email");
                            return usuarioDF.crearUsuario(nombre, email);
                        })
                        .dataFetcher("actualizarUsuario", environment -> {
                            int id = Integer.parseInt(environment.getArgument("id"));
                            String nombre = environment.getArgument("nombre");
                            String email = environment.getArgument("email");
                            return usuarioDF.actualizarUsuario(id, nombre, email);
                        })
                        .dataFetcher("eliminarUsuario", environment -> {
                            int id = Integer.parseInt(environment.getArgument("id"));
                            return usuarioDF.eliminarUsuario(id);
                        }))
                .build();
    }

    /**
     * Retorna la instancia configurada de GraphQL.
     */
    public GraphQL getGraphQL() {
        return graphQL;
    }
}
// Fin del código