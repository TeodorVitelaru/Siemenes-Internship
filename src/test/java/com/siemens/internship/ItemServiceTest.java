package com.siemens.internship;

import com.siemens.internship.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item item;

    @BeforeEach
    public void setup() {
        item = new Item();
        item.setId(1L);
        item.setName("Laptop");
        item.setDescription("Lenovo Thinkpad");
        item.setStatus("NEW");
        item.setEmail("test@email.com");
    }

    @Test
    public void testSaveItem() {
        when(itemRepository.save(item)).thenReturn(item);

        Item saved = itemService.save(item);

        assertNotNull(saved);
        assertEquals("Laptop", saved.getName());
    }

    @Test
    public void testFindById_Found() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Optional<Item> result = itemService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Laptop", result.get().getName());
    }

    @Test
    public void testFindById_NotFound() {
        when(itemRepository.findById(2L)).thenReturn(Optional.empty());

        Optional<Item> result = itemService.findById(2L);

        assertFalse(result.isPresent());
    }

    @Test
    public void testDeleteById_Success() {
        when(itemRepository.existsById(1L)).thenReturn(true);

        itemService.deleteById(1L);

        verify(itemRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testDeleteById_NotFound() {
        when(itemRepository.existsById(2L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> itemService.deleteById(2L));
    }

    @Test
    public void testUpdateItem_Success() {
        Item updated = new Item(1L, "Monitor", "Dell 24", "UPDATED", "mail@dell.com");

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(updated);

        Item result = itemService.updateItem(1L, updated);

        assertEquals("Monitor", result.getName());
        assertEquals("mail@dell.com", result.getEmail());
    }

    @Test
    public void testUpdateItem_NotFound() {
        when(itemRepository.findById(2L)).thenReturn(Optional.empty());

        Item newItem = new Item();
        assertThrows(ResourceNotFoundException.class, () -> itemService.updateItem(2L, newItem));
    }

    @Test
    public void testProcessItemsAsync() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);
        Item item1 = new Item(1L, "Item1", "Desc1", "NEW", "a@a.com");
        Item item2 = new Item(2L, "Item2", "Desc2", "NEW", "b@b.com");

        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Item> processed = itemService.processItemsAsync();

        // Assert
        assertEquals(2, processed.size());
        assertEquals("PROCESSED", processed.get(0).getStatus());
    }

}
