package com.customersupport.room;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class RoomRepository implements PanacheRepository<Room> {

    public Optional<Room> findByName(String name) {
        return find("name", name).firstResultOptional();
    }
}
