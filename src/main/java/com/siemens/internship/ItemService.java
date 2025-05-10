package com.siemens.internship;

import com.siemens.internship.exceptions.ResourceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service class that implements business logic for Item operations.
 */
@Service
@EnableAsync
public class ItemService {
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository itemRepository;

    // Thread pool for async operations with proper shutdown handling
    private static final ExecutorService executor = Executors.newFixedThreadPool(10, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // Allow JVM to exit if tasks are still running
        return t;
    });

    @Autowired
    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * Retrieves all items from the database.
     *
     * @return List of all items
     */
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    /**
     * Finds an item by its ID.
     *
     * @param id The ID of the item to find
     * @return Optional containing the item if found, empty otherwise
     */
    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    /**
     * Retrieves an item by ID or throws an exception if not found.
     *
     * @param id The ID of the item to find
     * @return The found item
     * @throws ResourceNotFoundException if item not found
     */
    public Item getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
    }

    /**
     * Saves an item to the database.
     *
     * @param item The item to save
     * @return The saved item with generated ID
     */
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    /**
     * Updates an existing item.
     *
     * @param id The ID of the item to update
     * @param itemDetails New item details
     * @return The updated item
     * @throws ResourceNotFoundException if item not found
     */
    public Item updateItem(Long id, Item itemDetails) {
        Item item = getItemById(id);

        item.setName(itemDetails.getName());
        item.setDescription(itemDetails.getDescription());
        item.setStatus(itemDetails.getStatus());
        item.setEmail(itemDetails.getEmail());

        return itemRepository.save(item);
    }

    /**
     * Deletes an item by its ID.
     *
     * @param id The ID of the item to delete
     * @throws ResourceNotFoundException if item not found
     */
    public void deleteById(Long id) {
        // Verify item exists before attempting to delete
        if (!itemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item not found with id: " + id);
        }
        itemRepository.deleteById(id);
    }

    /**
     * Processes items asynchronously.
     *
     * This method asynchronously processes all items by:
     * 1. Retrieving all item IDs
     * 2. Creating a CompletableFuture for each item to process it
     * 3. Waiting for all futures to complete
     * 4. Collecting the results
     *
     * The method ensures thread safety and proper coordination of async tasks.
     *
     * @return List of successfully processed items
     */
    public List<Item> processItemsAsync() {
        try {
            List<Long> itemIds = itemRepository.findAllIds();
            List<CompletableFuture<Item>> futures = new ArrayList<>();

            for (Long id : itemIds) {
                CompletableFuture<Item> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(100);  // Simulăm un timp de procesare
                        Optional<Item> itemOpt = itemRepository.findById(id);

                        if (itemOpt.isEmpty()) {
                            logger.warn("Item not found with id: {}", id);
                            return null;
                        }

                        Item item = itemOpt.get();
                        item.setStatus("PROCESSED");
                        return itemRepository.save(item);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Processing interrupted for item id: {}", id);
                        return null;
                    } catch (Exception e) {
                        logger.error("Error processing item id: {}: {}", id, e.getMessage());
                        return null;
                    }
                }, executor);
                futures.add(future);
            }

            // Așteaptă finalizarea tuturor futures
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            return allOf.thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .filter(item -> item != null)
                            .collect(Collectors.toList()))
                    .get(30, TimeUnit.SECONDS); // Timeout de 30 secunde

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Error during async processing: {}", e.getMessage());
            throw new RuntimeException("Error during async processing", e);
        }
    }

}