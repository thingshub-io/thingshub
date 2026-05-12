package io.thingshub.acl;

public record PolicyModel(String subject, String source, AclAction action, AclType aclType) {

}
