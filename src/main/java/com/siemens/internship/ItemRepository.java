package com.siemens.internship;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    /**
     * Custom query to find all item IDs.
     *
     * @return a list of all item IDs in the database
     */
    @Query("SELECT i.id FROM Item i")
    List<Long> findAllIds();
}
