package com.systems.fele.memendex_server.model;

public record Tag(long id, String scope, String value) {
    public static Tag parse(String tag) {
        if (tag.charAt(0) == '#') tag = tag.substring(1);
        var i = tag.indexOf('/');
        if (i < 0) return new Tag(0, tag, null);
        return new Tag(0, tag.substring(0, i), tag.substring(i + 1));
    }

    @Override
    public String toString() {
        return "#" + (value == null ? scope : scope + "/" + value);
    }
}
