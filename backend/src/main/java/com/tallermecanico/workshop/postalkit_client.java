package com.tallermecanico.workshop;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Proxy autenticado y con caché en memoria para la consulta postal mexicana. */
@Service
public class postalkit_client {
    private final RestClient rest;
    private final String api_key;
    private final ConcurrentMap<String, JsonNode> cache = new ConcurrentHashMap<>();
    /** Configura el cliente HTTP y la credencial sin exponerla al navegador. */
    public postalkit_client(RestClient.Builder builder, @Value("${app.postalkit.base-url}") String base_url, @Value("${app.postalkit.api-key:}") String api_key){this.rest=builder.baseUrl(base_url).build();this.api_key=api_key;}
    /** Consulta y cachea el catálogo de estados. */
    public JsonNode states(){return get("/v1/states");}
    /** Consulta y cachea los municipios de un estado. */
    public JsonNode municipalities(String state_id){return get("/v1/states/"+enc(state_id)+"/municipalities");}
    /** Consulta y cachea colonias y municipio a partir del código postal. */
    public JsonNode postal_code(String code){return get("/v1/postal-codes/"+enc(code));}
    /** Busca códigos postales que correspondan a la colonia, municipio y estado elegidos. */
    public JsonNode postal_search(String state, String municipality, String colonia){return get("/v1/postal-codes/search?state="+enc(state)+"&municipality="+enc(municipality)+"&colonia="+enc(colonia));}
    /** Obtiene una respuesta JSON del proveedor con Bearer y evita repetir consultas en memoria. */
    private JsonNode get(String path){
        if(api_key==null||api_key.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configura POSTALKIT_API_KEY para habilitar la consulta de estados, municipios y códigos postales.");
        return cache.computeIfAbsent(path,key->{try{return rest.get().uri(key).header(HttpHeaders.AUTHORIZATION,"Bearer "+api_key).retrieve().body(JsonNode.class);}catch(Exception error){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"No fue posible consultar la API postal.");}});
    }
    /** Codifica cada valor utilizado como segmento o parámetro de URL. */
    private String enc(String value){return java.net.URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8);}
}
