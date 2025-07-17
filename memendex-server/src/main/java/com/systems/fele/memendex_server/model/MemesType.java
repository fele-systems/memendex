package com.systems.fele.memendex_server.model;

import java.util.stream.Stream;

public enum MemesType {
    file(1),
    link(2),
    note(3);

    final int id;

    MemesType(int id) {
        this.id = id;
    }

    public static MemesType fromId(int typeId) {
        if (typeId == file.id)
            return file;
        else if (typeId == link.id)
            return link;
        else if (typeId == note.id)
            return note;
        else
            throw new IllegalArgumentException("Invalid meme type id: " + typeId);
    }

    public int getId() {
        return id;
    }
}
