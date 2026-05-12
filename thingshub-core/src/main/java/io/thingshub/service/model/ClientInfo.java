package io.thingshub.service.model;

public record ClientInfo(String tenant, String clientId, ClientType clientType, String username) {

}
