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
            // Get all item IDs
            List<Long> itemIds = itemRepository.findAllIds();

            // Create a list to hold all futures
            List<CompletableFuture<Item>> futures = new ArrayList<>();

            // Create a future for each item
            for (Long id : itemIds) {
                CompletableFuture<Item> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        // Simulating some processing time
                        Thread.sleep(100);

                        // Find the item
                        Optional<Item> optionalItem = itemRepository.findById(id);
                        if (optionalItem.isEmpty()) {
                            logger.warn("Item with id {} not found during async processing", id);
                            return null;
                        }

                        Item item = optionalItem.get();
                        item.setStatus("PROCESSED");

                        // Save the updated item
                        return itemRepository.save(item);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Interrupted during item processing: {}", e.getMessage());
                        return null;
                    } catch (Exception e) {
                        logger.error("Error processing item with id {}: {}", id, e.getMessage());
                        return null;
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all futures to complete and collect results
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            // Get the results after all futures complete
            return allFutures.thenApply(v ->
                    futures.stream()
                            .map(CompletableFuture::join)
                            .filter(item -> item != null)
                            .collect(Collectors.toList())
            ).get(30, TimeUnit.SECONDS); // Set a reasonable timeout

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Process was interrupted: {}", e.getMessage());
            throw new RuntimeException("Process was interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Execution error during async processing: {}", e.getMessage());
            throw new RuntimeException("Error during async processing", e.getCause());
        } catch (TimeoutException e) {
            logger.error("Timeout occurred during async processing: {}", e.getMessage());
            throw new RuntimeException("Timeout during async processing", e);
        }
    }
}