package com.siemens.internship;

import com.siemens.internship.exceptions.ErrorDetails;
import com.siemens.internship.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    /**
     * Retrieves all items
     *
     * @return ResponseEntity with list of all items
     */
    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return new ResponseEntity<>(itemService.findAll(), HttpStatus.OK);
    }

    /**
     * Creates a new item
     *
     * @param item The item to create
     * @param result Validation result
     * @return ResponseEntity with created item or errors
     */
    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });

            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(itemService.save(item), HttpStatus.CREATED);
    }

    /**
     * Retrieves an item by ID
     *
     * @param id The ID of the item to retrieve
     * @return ResponseEntity with item if found, 404 otherwise
     */
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        try {
            Item item = itemService.getItemById(id);
            return new ResponseEntity<>(item, HttpStatus.OK);
        } catch (ResourceNotFoundException ex) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Updates an existing item
     *
     * @param id The ID of the item to update
     * @param item The updated item details
     * @return ResponseEntity with updated item or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });

            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        try {
            Item updatedItem = itemService.updateItem(id, item);
            return new ResponseEntity<>(updatedItem, HttpStatus.OK);
        } catch (ResourceNotFoundException ex) {
            return new ResponseEntity<>(
                    new ErrorDetails(new Date(), ex.getMessage(), "Item update failed"),
                    HttpStatus.NOT_FOUND
            );
        }
    }

    /**
     * Deletes an item by ID
     *
     * @param id The ID of the item to delete
     * @return ResponseEntity with no content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        try {
            itemService.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (ResourceNotFoundException ex) {
            return new ResponseEntity<>(
                    new ErrorDetails(new Date(), ex.getMessage(), "Item deletion failed"),
                    HttpStatus.NOT_FOUND
            );
        }
    }

    /**
     * Processes all items asynchronously
     *
     * @return ResponseEntity with list of processed items
     */
    @GetMapping("/process")
    public ResponseEntity<List<Item>> processItems() {
        try {
            List<Item> processedItems = itemService.processItemsAsync();
            return new ResponseEntity<>(processedItems, HttpStatus.OK);
        } catch (Exception ex) {
            // Log the exception here
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Process items asynchronously without blocking the response
     *
     * @return Immediate response with processing status
     */
    @GetMapping("/process-async")
    public ResponseEntity<?> processItemsAsynchronously() {
        CompletableFuture.runAsync(() -> itemService.processItemsAsync());

        Map<String, String> response = new HashMap<>();
        response.put("status", "Processing started");
        response.put("message", "Items are being processed asynchronously");

        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }
}