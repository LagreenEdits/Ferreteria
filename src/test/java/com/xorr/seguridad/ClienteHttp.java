package com.xorr.seguridad;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.jayway.jsonpath.JsonPath;

// Cliente HTTP real (sin MockMvc) para las pruebas de integración de seguridad:
// así se ejecuta la cadena de filtros completa, incluido el paso por /error que usa
// Spring al responder 405, que MockMvc no reproduce.
class ClienteHttp {

    private final HttpClient cliente = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final String base;

    ClienteHttp(int puerto) {
        this.base = "http://localhost:" + puerto;
    }

    HttpResponse<String> get(String ruta, String token) throws Exception {
        return llamar("GET", ruta, token, null);
    }

    HttpResponse<String> post(String ruta, String token, String cuerpo) throws Exception {
        return llamar("POST", ruta, token, cuerpo);
    }

    HttpResponse<String> put(String ruta, String token, String cuerpo) throws Exception {
        return llamar("PUT", ruta, token, cuerpo);
    }

    HttpResponse<String> delete(String ruta, String token) throws Exception {
        return llamar("DELETE", ruta, token, null);
    }

    HttpResponse<String> llamar(String metodo, String ruta, String token, String cuerpo) throws Exception {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(base + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, cuerpo == null ? BodyPublishers.noBody() : BodyPublishers.ofString(cuerpo));
        if (token != null) {
            pedido.header("Authorization", "Bearer " + token);
        }
        return cliente.send(pedido.build(), BodyHandlers.ofString());
    }

    static String texto(HttpResponse<String> respuesta, String rutaJson) {
        return JsonPath.read(respuesta.body(), rutaJson);
    }

    static long numero(HttpResponse<String> respuesta, String rutaJson) {
        Number valor = JsonPath.read(respuesta.body(), rutaJson);
        return valor.longValue();
    }
}