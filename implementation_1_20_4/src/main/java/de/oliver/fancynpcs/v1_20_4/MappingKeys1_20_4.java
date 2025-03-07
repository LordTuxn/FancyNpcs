package de.oliver.fancynpcs.v1_20_4;

public enum MappingKeys1_20_4 {

    ENTITY_TYPE__FACTORY("bC"),
    SYNCHED_ENTITY_DATA__ITEMS_BY_ID("e"),
    CLIENTBOUND_TELEPORT_ENTITY_PACKET__X("b"),
    CLIENTBOUND_TELEPORT_ENTITY_PACKET__Y("c"),
    CLIENTBOUND_TELEPORT_ENTITY_PACKET__Z("d"),
    ;

    private final String mapping;

    MappingKeys1_20_4(String mapping) {
        this.mapping = mapping;
    }

    public String getMapping() {
        return mapping;
    }
}
