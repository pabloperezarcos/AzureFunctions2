package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import java.util.logging.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Función serverless en Azure que consume eventos desde Event Grid.
 */
public class ConsumirEventosFunction {

    @FunctionName("ConsumirEventos")
    public void run(
            @EventGridTrigger(name = "eventGridEvent") String content,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("🔔 Función con Event Grid Trigger ejecutada.");

        // Deserializar el contenido del evento
        Gson gson = new Gson();
        JsonObject eventGridEvent = gson.fromJson(content, JsonObject.class);

        // Log completo
        logger.info("Evento recibido (JSON completo): " + eventGridEvent.toString());

        // Extraer tipo y datos
        String eventType = eventGridEvent.get("eventType").getAsString();
        String data = eventGridEvent.get("data").toString();

        // Mostrar en log
        logger.info("📝 Tipo de evento: " + eventType);
        logger.info("📦 Data del evento: " + data);
    }
}
