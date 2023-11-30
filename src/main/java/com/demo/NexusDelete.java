package com.demo;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class NexusDelete implements Runnable {
	
	private static final Logger log = Logger.getLogger(NexusDelete.class.getSimpleName());
	
	private static final String nexusUser = "";
	private static final String nexusPass = "";
	private static final String nexusUrl 	= "";
	
	private static final String NEXUS_VALUE_REPOSITORY 		= "nuget.org-proxy";
	private static final String NEXUS_VALUE_FORMAT     		= "nuget";
	private static final String NEXUS_VALUE_QUERY     		= "q";
	
	private static final String NEXUS_CAMPO_ITEMS   		= "items";
	private static final String NEXUS_CAMPO_ID				= "id";
	private static final String NEXUS_CAMPO_VERSION 		= "version";
	private static final String NEXUS_CAMPO_REPOSITORY 		= "repository";
	private static final String NEXUS_CAMPO_FORMAT 			= "format";
	private static final String NEXUS_REST_API_STATUS		= "status";
	private static final String NEXUS_REST_API_SERVICE		= "service";
	private static final String NEXUS_REST_API_REST			= "rest";
	private static final String NEXUS_REST_API_VERSION		= "v1";
	private static final String NEXUS_REST_API_SEARCH		= "search";
	private static final String NEXUS_REST_API_COMPONENTS	= "components";


	public NexusDelete() {

		Thread t = new Thread(this, "general delete method");
		t.start();
	}

	private void validateServiceConnection() throws ParseException {
		final WebResource service = getService();
		final String nexusStatus = service.path(NEXUS_REST_API_SERVICE)
				.path(NEXUS_REST_API_REST)
				.path(NEXUS_REST_API_VERSION)
				.path(NEXUS_REST_API_STATUS)
				.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class).toString();
		log.info(nexusStatus + "\n");

		ambuscadesRepository(service, "-rc.");
		//ambuscadesRepository(service, "~rc.");
	}
	
    private static void ambuscadesRepository(WebResource service, String versionFormat) {
		ObjectMapper objectMapper = new ObjectMapper();
		while (true){
			String repo = service.path(NEXUS_REST_API_SERVICE).path(NEXUS_REST_API_REST).path(NEXUS_REST_API_VERSION).path(NEXUS_REST_API_SEARCH)
					.queryParam(NEXUS_CAMPO_REPOSITORY, "nuget.org-proxy")
					//.queryParam(NEXUS_CAMPO_FORMAT, "maven2")
					//.queryParam(NEXUS_VALUE_QUERY, "rc")
					.accept(MediaType.APPLICATION_JSON).get(String.class);
			try {
				JsonNode jsonNode = objectMapper.readTree(repo);
				JsonNode jsonNodeItem = jsonNode.get(NEXUS_CAMPO_ITEMS);
				if(jsonNodeItem.size() == 0) {
					break;
				}
				for (JsonNode jsn : jsonNodeItem) {
					//log.info(jsn.get(NEXUS_CAMPO_ID).asText());
					log.info(jsn.get(NEXUS_CAMPO_VERSION).asText());
					if(jsn.get(NEXUS_CAMPO_VERSION).asText().contains("rc")) {
						eliminateComponent(service, jsn.get(NEXUS_CAMPO_ID).asText(), jsn.get(NEXUS_CAMPO_VERSION).asText());
					}
				}
			}catch (IOException e) {
				log.warning(e.getMessage());
			}
		}
	}

	private static void eliminateComponent(WebResource service, String id, String version) throws IOException {
		final String statusCode = service.path(NEXUS_REST_API_SERVICE)
				.path(NEXUS_REST_API_REST)
				.path(NEXUS_REST_API_VERSION)
				.path(NEXUS_REST_API_COMPONENTS)
				.path(id).accept(MediaType.APPLICATION_JSON)
				.delete(ClientResponse.class).toString();
		log.warning(" version "+version+" --- "+statusCode);
	}

	private static WebResource getService() {
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
		client.addFilter(new HTTPBasicAuthFilter(nexusUser, nexusPass));
		return client.resource(getBaseURI());
	}
    
    private static URI getBaseURI() {
		return UriBuilder.fromUri(nexusUrl).build();
	}
    
    public void run() {
    	try {
			validateServiceConnection();
		} catch (ParseException e) {
			e.printStackTrace();
		}
    }
    
}
